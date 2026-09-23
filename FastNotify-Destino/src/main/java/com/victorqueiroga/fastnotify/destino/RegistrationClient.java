package com.victorqueiroga.fastnotify.destino;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RegistrationClient {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int SO_TIMEOUT_MS = 5000;

    private RegistrationClient() {
    }

    public static Protocol.Ack register(String host, int registerPort,
                                        Protocol.Registration reg, String psk) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, registerPort), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SO_TIMEOUT_MS);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            Protocol.writeRegistration(out, reg, psk);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            return Protocol.readAck(in, psk);
        }
    }
}
