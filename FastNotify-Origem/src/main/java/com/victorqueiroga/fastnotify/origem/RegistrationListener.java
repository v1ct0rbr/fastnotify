package com.victorqueiroga.fastnotify.origem;

import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class RegistrationListener {
    private static final int ACCEPT_TIMEOUT_MS = 1000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final IntSupplier portSupplier;
    private final Supplier<String> tokenSupplier;
    private final Supplier<String> pskSupplier;
    private final SenderView view;
    private final DestinosStore destinosStore;
    private volatile boolean running;
    private volatile Thread thread;
    private volatile ServerSocket serverSocket;

    public RegistrationListener(IntSupplier portSupplier, Supplier<String> tokenSupplier,
                                 Supplier<String> pskSupplier, SenderView view,
                                 DestinosStore destinosStore) {
        this.portSupplier = portSupplier;
        this.tokenSupplier = tokenSupplier;
        this.pskSupplier = pskSupplier;
        this.view = view;
        this.destinosStore = destinosStore;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        Thread t = new Thread(this::loop, "fastnotify-registration");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    public synchronized void stop() {
        running = false;
        ServerSocket ss = serverSocket;
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException ignored) {
            }
            serverSocket = null;
        }
        Thread t = thread;
        if (t != null) {
            t.interrupt();
            thread = null;
        }
    }

    public synchronized void restart() {
        stop();
        start();
    }

    private void loop() {
        int lastPort = -1;
        while (running) {
            int port = portSupplier.getAsInt();
            if (port != lastPort || serverSocket == null) {
                if (!openServer(port)) {
                    return;
                }
                lastPort = port;
                int p = port;
                runFx(() -> {
                    view.setRegisterStatus("Cadastro: porta " + p);
                    view.logAs(LogType.REGISTER, "-",
                            "Escutando cadastros na porta " + p + " ...");
                });
            }
            ServerSocket ss = serverSocket;
            if (ss == null || ss.isClosed()) {
                return;
            }
            try {
                ss.setSoTimeout(ACCEPT_TIMEOUT_MS);
                Socket socket = ss.accept();
                handle(socket);
            } catch (java.net.SocketTimeoutException ignored) {
            } catch (IOException e) {
                if (running) {
                    runFx(() -> view.logAs(LogType.ERROR, "-",
                            "ERRO cadastro — " + e.getMessage()));
                }
            }
        }
    }

    private boolean openServer(int port) {
        closeServer();
        try {
            ServerSocket ss = new ServerSocket(port, 50,
                    InetAddress.getByName("0.0.0.0"));
            serverSocket = ss;
            return true;
        } catch (IOException e) {
            runFx(() -> view.logAs(LogType.ERROR, "-",
                    "ERRO ao escutar cadastros na porta " + port + " — " + e.getMessage()));
            running = false;
            return false;
        }
    }

    private void closeServer() {
        ServerSocket ss = serverSocket;
        serverSocket = null;
        if (ss != null) {
            try {
                ss.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handle(Socket socket) {
        String ip = clientIp(socket);
        String psk = pskSupplier.get();
        try (socket) {
            socket.setSoTimeout(READ_TIMEOUT_MS);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            Protocol.Registration reg = Protocol.readRegistration(in, psk);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String expected = nullToEmpty(tokenSupplier.get());
            String actual = nullToEmpty(reg.tokenOrigem());
            if (!tokenMatches(expected, actual)) {
                Protocol.writeAck(out, false, "Token da origem inválido", psk);
                runFx(() -> view.logAs(LogType.REGISTER, ip,
                        "CADASTRO RECUSADO — token inválido"));
                return;
            }

            int port = reg.port();
            if (port < 1 || port > 65535) {
                Protocol.writeAck(out, false, "Porta de destino inválida", psk);
                runFx(() -> view.logAs(LogType.REGISTER, ip,
                        "CADASTRO RECUSADO — porta inválida"));
                return;
            }

            String socketIp = clientIp(socket);
            String reportedName = nullToEmpty(reg.hostName()).trim();
            String reportedIp = nullToEmpty(reg.hostIp()).trim();
            String hostIp = !socketIp.isEmpty() ? socketIp : reportedIp;
            String connectHost = !reportedName.isEmpty() ? reportedName
                    : (!hostIp.isEmpty() ? hostIp : socketIp);

            DestinosStore.Destino destino = new DestinosStore.Destino(
                    reg.alias(), connectHost, port, reg.tokenDestino(), reg.screen(),
                    reg.durationMs(), reg.department(), hostIp)
                    .normalized();
            boolean created = destinosStore.upsertByHost(reportedName, hostIp, destino);
            Protocol.writeAck(out, true, created ? "Cadastrado" : "Atualizado", psk);
            runFx(() -> {
                view.upsertDestino(destino);
                view.logAs(LogType.REGISTER, hostIp,
                        (created ? "Cadastro" : "Atualização") + " auto — alias="
                                + destino.alias()
                                + " host=" + destino.connectHost()
                                + (destino.effectiveHostIp().isEmpty()
                                || destino.effectiveHostIp().equalsIgnoreCase(
                                destino.connectHost())
                                ? "" : " ip=" + destino.effectiveHostIp())
                                + " dept=" + (destino.effectiveDepartment().isEmpty()
                                ? "-" : destino.effectiveDepartment())
                                + " porta=" + port);
            });
        } catch (IOException e) {
            runFx(() -> view.logAs(LogType.ERROR, ip,
                    "CADASTRO FALHOU — " + e.getMessage()));
        }
    }

    private static String clientIp(Socket socket) {
        InetAddress addr = socket.getInetAddress();
        return addr == null ? "" : addr.getHostAddress();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean tokenMatches(String expected, String actual) {
        if (expected == null || expected.isEmpty()) {
            return false;
        }
        try {
            MessageDigest a = MessageDigest.getInstance("SHA-256");
            byte[] ha = a.digest(expected.getBytes(StandardCharsets.UTF_8));
            MessageDigest b = MessageDigest.getInstance("SHA-256");
            byte[] hb = b.digest(actual.getBytes(StandardCharsets.UTF_8));
            return MessageDigest.isEqual(ha, hb);
        } catch (Exception e) {
            return expected.equals(actual);
        }
    }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
