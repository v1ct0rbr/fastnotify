package com.victorqueiroga.fastnotify.destino;

import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ListenerService {
    private static final int ACCEPT_TIMEOUT_MS = 1000;
    private static final int READ_TIMEOUT_MS = 3000;

    private final Supplier<Integer> portSupplier;
    private final Supplier<String> tokenSupplier;
    private final Supplier<String> pskSupplier;
    private final Supplier<Boolean> soundEnabledSupplier;
    private final BiConsumer<NotificationMessage, String> onMessage;
    private final LogService logService;
    private final IpBlockManager blockManager = new IpBlockManager();
    private volatile boolean running;
    private Thread thread;
    private ServerSocket serverSocket;

    public ListenerService(Supplier<Integer> portSupplier,
                           Supplier<String> tokenSupplier,
                           Supplier<String> pskSupplier,
                           Supplier<Boolean> soundEnabledSupplier,
                           BiConsumer<NotificationMessage, String> onMessage,
                           LogService logService) {
        this.portSupplier = portSupplier;
        this.tokenSupplier = tokenSupplier;
        this.pskSupplier = pskSupplier;
        this.soundEnabledSupplier = soundEnabledSupplier;
        this.onMessage = onMessage;
        this.logService = logService;
    }

    public IpBlockManager getBlockManager() {
        return blockManager;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        thread = new Thread(this::loop, "fastnotify-listener");
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    private void loop() {
        int port = portSupplier.get();
        try (ServerSocket ss = new ServerSocket(port)) {
            serverSocket = ss;
            ss.setSoTimeout(ACCEPT_TIMEOUT_MS);
            record(LogType.INFO, "-", "Escutando na porta " + port, null);
            while (running) {
                try (Socket socket = ss.accept()) {
                    String ip = clientIp(socket);
                    socket.setSoTimeout(READ_TIMEOUT_MS);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    NotificationMessage msg = Protocol.read(in, pskSupplier.get());
                    handle(socket, ip, msg);
                } catch (SocketTimeoutException ignored) {
                } catch (IOException e) {
                    if (running) {
                        record(LogType.ERROR, "-", "Erro de conexão: " + e.getMessage(), null);
                    }
                }
            }
        } catch (IOException e) {
            record(LogType.ERROR, "-", "Falha ao abrir porta " + port + ": " + e.getMessage(), null);
            running = false;
        }
    }

    private void handle(Socket socket, String ip, NotificationMessage msg) throws IOException {
        String expected = tokenSupplier.get() == null ? "" : tokenSupplier.get();
        String actual = msg.getToken() == null ? "" : msg.getToken();
        String psk = pskSupplier.get();
        boolean tokenOk = tokenMatches(expected, actual);

        if (!tokenOk && blockManager.isBlocked(ip)) {
            if (msg.isTest()) {
                String reason = blockManager.blockReason(ip);
                Protocol.writeAck(new DataOutputStream(socket.getOutputStream()),
                        false, reason, psk);
            }
            return;
        }

        if (tokenOk) {
            boolean hadBlockState = blockManager.currentBlockLevel(ip) > 0
                    || blockManager.invalidAttempts(ip) > 0
                    || blockManager.isBlocked(ip);
            blockManager.onSuccess(ip);
            if (hadBlockState) {
                record(LogType.AUTH_OK, ip,
                        "Token válido — tentativas/bloqueio reiniciados", null);
            }

            if (msg.isTest()) {
                Protocol.writeAck(new DataOutputStream(socket.getOutputStream()),
                        true, "OK", psk);
                record(LogType.TEST_OK, ip, "Teste de conexão OK (token válido)", null);
                return;
            }
            String body = "Recebida [" + msg.getType().label() + "] "
                    + (msg.getTitle().isBlank() ? msg.getBody() : msg.getTitle());
            record(LogType.MESSAGE, ip, body, msg);
            boolean sound = Boolean.TRUE.equals(soundEnabledSupplier.get());
            Platform.runLater(() -> new NotificationWindow(msg, sound));
            return;
        }

        long blockedSeconds = blockManager.onFailure(ip);
        int attempts = blockManager.invalidAttempts(ip);

        if (msg.isTest()) {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            if (blockedSeconds > 0) {
                String detail = "Token inválido — IP bloqueado por " + blockedSeconds + "s";
                Protocol.writeAck(out, false, detail, psk);
                record(LogType.TEST_FAIL, ip, "Teste FALHOU | " + detail
                        + " (após " + IpBlockManager.ATTEMPTS_THRESHOLD + " tentativas)", null);
                record(LogType.BLOCK, ip, "BLOQUEIO " + blockedSeconds + "s ativado", null);
            } else {
                String detail = "Token inválido (" + attempts + "/"
                        + IpBlockManager.ATTEMPTS_THRESHOLD + ")";
                Protocol.writeAck(out, false, detail, psk);
                record(LogType.TEST_FAIL, ip, "Teste FALHOU | " + detail, null);
            }
            return;
        }

        if (blockedSeconds > 0) {
            record(LogType.REJECT, ip, "Notificação rejeitada | token inválido | BLOQUEIO "
                    + blockedSeconds + "s (após " + IpBlockManager.ATTEMPTS_THRESHOLD
                    + " tentativas)", null);
            record(LogType.BLOCK, ip, "BLOQUEIO " + blockedSeconds + "s ativado", null);
        } else {
            record(LogType.REJECT, ip, "Notificação REJEITADA (token inválido) | tentativas "
                    + attempts + "/" + IpBlockManager.ATTEMPTS_THRESHOLD, null);
        }
    }

    private void record(LogType type, String ip, String message, NotificationMessage msg) {
        if (logService != null) {
            logService.append(type, ip, message);
        }
        onMessage.accept(msg, message);
    }

    private static String clientIp(Socket socket) {
        if (socket.getInetAddress() == null) {
            return "unknown";
        }
        return socket.getInetAddress().getHostAddress();
    }

    private static boolean tokenMatches(String expected, String actual) {
        if (expected.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
