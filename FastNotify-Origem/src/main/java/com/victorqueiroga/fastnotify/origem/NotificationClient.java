package com.victorqueiroga.fastnotify.origem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NotificationClient {
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int SO_TIMEOUT_MS = 3000;

    private NotificationClient() {
    }

    public static void send(String host, int port, NotificationMessage message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SO_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Protocol.write(out, message);
        }
    }

    public static Protocol.Ack test(String host, int port, String token) throws IOException {
        NotificationMessage msg = new NotificationMessage();
        msg.setToken(token);
        msg.setTest(true);
        msg.setTitle("TESTE");
        msg.setBody("Verificação de conexão");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SO_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Protocol.write(out, msg);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            return Protocol.readAck(in);
        }
    }
}
