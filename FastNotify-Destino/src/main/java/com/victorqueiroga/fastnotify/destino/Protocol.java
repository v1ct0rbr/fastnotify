package com.victorqueiroga.fastnotify.destino;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class Protocol {
    public static final String MAGIC = "FASTNOTIFY/1";
    public static final String ACK_MAGIC = "FASTNOTIFY/ACK";
    public static final String ACK_MAGIC_ENC = "FASTNOTIFY/ACK/E";
    public static final String REG_MAGIC = "FASTNOTIFY/REG";
    public static final int VERSION = 6;
    public static final int VERSION_ENCRYPTED = 7;
    public static final int VERSION_SENDER = 4;
    public static final int VERSION_ENCRYPTED_SENDER = 5;
    public static final int VERSION_LEGACY = 2;
    public static final int VERSION_ENCRYPTED_LEGACY = 3;
    public static final int REG_VERSION = 4;
    public static final int REG_VERSION_ENCRYPTED = 5;
    public static final int REG_VERSION_HOST = 4;
    public static final int REG_VERSION_LEGACY = 2;
    public static final int REG_VERSION_ENCRYPTED_LEGACY = 3;
    private static final int MAX_BLOB = 8 * 1024 * 1024;

    private Protocol() {
    }

    public static void write(DataOutputStream out, NotificationMessage msg, String psk)
            throws IOException {
        out.writeUTF(MAGIC);
        if (!Crypto.isEncrypted(psk)) {
            out.writeInt(VERSION);
            writeNotificationFields(out, msg);
        } else {
            out.writeInt(VERSION_ENCRYPTED);
            writeEncryptedBody(out, psk, p -> writeNotificationFields(p, msg));
        }
        out.flush();
    }

    public static NotificationMessage read(DataInputStream in, String psk) throws IOException {
        String magic = in.readUTF();
        if (!MAGIC.equals(magic)) {
            throw new IOException("Protocolo inválido: " + magic);
        }
        int version = in.readInt();
        DataInputStream body = bodyStream(in, version, psk);
        NotificationMessage msg = new NotificationMessage();
        msg.setToken(body.readUTF());
        msg.setType(MsgType.fromCode(body.readUTF()));
        msg.setDurationMs(body.readLong());
        msg.setScreenIndex(body.readInt());
        msg.setTest(body.readBoolean());
        msg.setTitle(body.readUTF());
        msg.setBody(body.readUTF());
        if (version >= VERSION_SENDER) {
            msg.setSenderFullName(body.readUTF());
        }
        if (version >= VERSION) {
            msg.setSenderDomain(body.readUTF());
        }
        return msg;
    }

    public static void writeRegistration(DataOutputStream out, Registration reg, String psk)
            throws IOException {
        out.writeUTF(REG_MAGIC);
        if (!Crypto.isEncrypted(psk)) {
            out.writeInt(REG_VERSION);
            writeRegistrationFields(out, reg);
        } else {
            out.writeInt(REG_VERSION_ENCRYPTED);
            writeEncryptedBody(out, psk, p -> writeRegistrationFields(p, reg));
        }
        out.flush();
    }

    public static Registration readRegistration(DataInputStream in, String psk)
            throws IOException {
        String magic = in.readUTF();
        if (!REG_MAGIC.equals(magic)) {
            throw new IOException("Protocolo de registro inválido: " + magic);
        }
        int version = in.readInt();
        boolean encrypted = Crypto.isEncrypted(psk);
        DataInputStream body;
        if (version == REG_VERSION || version == REG_VERSION_HOST
                || version == REG_VERSION_LEGACY) {
            if (encrypted) {
                throw new IOException(
                        "Mensagem em claro — a PSK está habilitada (esperava cifra)");
            }
            body = in;
        } else if (version == REG_VERSION_ENCRYPTED || version == REG_VERSION_ENCRYPTED_LEGACY) {
            if (!encrypted) {
                throw new IOException(
                        "Mensagem cifrada — configure a mesma PSK nos dois lados");
            }
            body = new DataInputStream(
                    new ByteArrayInputStream(readEncryptedBlob(in, psk)));
        } else {
            throw new IOException("Versão incompatível: " + version);
        }
        String tokenOrigem = body.readUTF();
        String alias = body.readUTF();
        int port = body.readInt();
        String tokenDestino = body.readUTF();
        int screen = body.readInt();
        long durationMs = body.readLong();
        String department = body.readUTF();
        String hostName = "";
        String hostIp = "";
        if (version >= REG_VERSION_HOST) {
            hostName = body.readUTF();
            hostIp = body.readUTF();
        }
        return new Registration(tokenOrigem, alias, port, tokenDestino, screen, durationMs,
                department, hostName, hostIp);
    }

    public static void writeAck(DataOutputStream out, boolean ok, String detail, String psk)
            throws IOException {
        String text = detail == null ? "" : detail;
        if (!Crypto.isEncrypted(psk)) {
            out.writeUTF(ACK_MAGIC);
            out.writeBoolean(ok);
            out.writeUTF(text);
        } else {
            out.writeUTF(ACK_MAGIC_ENC);
            writeEncryptedBody(out, psk, p -> {
                p.writeBoolean(ok);
                p.writeUTF(text);
            });
        }
        out.flush();
    }

    public static Ack readAck(DataInputStream in, String psk) throws IOException {
        String magic = in.readUTF();
        if (ACK_MAGIC.equals(magic)) {
            if (Crypto.isEncrypted(psk)) {
                throw new IOException("ACK em claro — a PSK está habilitada");
            }
            boolean ok = in.readBoolean();
            String detail = in.readUTF();
            return new Ack(ok, detail);
        }
        if (ACK_MAGIC_ENC.equals(magic)) {
            if (!Crypto.isEncrypted(psk)) {
                throw new IOException("ACK cifrado — configure a mesma PSK nos dois lados");
            }
            byte[] plain = readEncryptedBlob(in, psk);
            DataInputStream body = new DataInputStream(new ByteArrayInputStream(plain));
            boolean ok = body.readBoolean();
            String detail = body.readUTF();
            return new Ack(ok, detail);
        }
        throw new IOException("ACK inválido: " + magic);
    }

    private interface BodyWriter {
        void writeTo(DataOutputStream out) throws IOException;
    }

    private static void writeEncryptedBody(DataOutputStream out, String psk, BodyWriter writer)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream payload = new DataOutputStream(bos)) {
            writer.writeTo(payload);
        }
        byte[] blob = Crypto.encrypt(psk, bos.toByteArray());
        out.writeInt(blob.length);
        out.write(blob);
    }

    private static byte[] readEncryptedBlob(DataInputStream in, String psk) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_BLOB) {
            throw new IOException("Payload cifrado inválido");
        }
        byte[] blob = in.readNBytes(length);
        if (blob.length != length) {
            throw new IOException("Payload cifrado truncado");
        }
        return Crypto.decrypt(psk, blob);
    }

    private static DataInputStream bodyStream(DataInputStream in, int version, String psk)
            throws IOException {
        boolean encrypted = Crypto.isEncrypted(psk);
        if (version == VERSION || version == VERSION_SENDER || version == VERSION_LEGACY) {
            if (encrypted) {
                throw new IOException(
                        "Mensagem em claro — a PSK está habilitada (esperava cifra)");
            }
            return in;
        }
        if (version == VERSION_ENCRYPTED || version == VERSION_ENCRYPTED_SENDER
                || version == VERSION_ENCRYPTED_LEGACY) {
            if (!encrypted) {
                throw new IOException(
                        "Mensagem cifrada — configure a mesma PSK nos dois lados");
            }
            return new DataInputStream(
                    new ByteArrayInputStream(readEncryptedBlob(in, psk)));
        }
        throw new IOException("Versão incompatível: " + version);
    }

    private static DataInputStream bodyStream(DataInputStream in, int version,
                                              int clearVersion, int encryptedVersion,
                                              String psk) throws IOException {
        if (version == clearVersion) {
            if (Crypto.isEncrypted(psk)) {
                throw new IOException(
                        "Mensagem em claro — a PSK está habilitada (esperava cifra)");
            }
            return in;
        }
        if (version == encryptedVersion) {
            if (!Crypto.isEncrypted(psk)) {
                throw new IOException(
                        "Mensagem cifrada — configure a mesma PSK nos dois lados");
            }
            return new DataInputStream(
                    new ByteArrayInputStream(readEncryptedBlob(in, psk)));
        }
        throw new IOException("Versão incompatível: " + version);
    }

    private static void writeNotificationFields(DataOutputStream out, NotificationMessage msg)
            throws IOException {
        out.writeUTF(msg.getToken() == null ? "" : msg.getToken());
        out.writeUTF(msg.getType().name());
        out.writeLong(msg.getDurationMs());
        out.writeInt(msg.getScreenIndex());
        out.writeBoolean(msg.isTest());
        out.writeUTF(msg.getTitle());
        out.writeUTF(msg.getBody());
        out.writeUTF(msg.getSenderFullName() == null ? "" : msg.getSenderFullName());
        out.writeUTF(msg.getSenderDomain() == null ? "" : msg.getSenderDomain());
    }

    private static void writeRegistrationFields(DataOutputStream out, Registration reg)
            throws IOException {
        out.writeUTF(reg.tokenOrigem() == null ? "" : reg.tokenOrigem());
        out.writeUTF(reg.alias() == null ? "" : reg.alias());
        out.writeInt(reg.port());
        out.writeUTF(reg.tokenDestino() == null ? "" : reg.tokenDestino());
        out.writeInt(reg.screen());
        out.writeLong(reg.durationMs());
        out.writeUTF(reg.department() == null ? "" : reg.department());
        out.writeUTF(reg.hostName() == null ? "" : reg.hostName());
        out.writeUTF(reg.hostIp() == null ? "" : reg.hostIp());
    }

    public record Ack(boolean ok, String detail) {
    }

    public record Registration(String tokenOrigem, String alias, int port,
                               String tokenDestino, int screen, long durationMs,
                               String department, String hostName, String hostIp) {

        public Registration(String tokenOrigem, String alias, int port,
                            String tokenDestino, int screen, long durationMs,
                            String department) {
            this(tokenOrigem, alias, port, tokenDestino, screen, durationMs,
                    department, "", "");
        }
    }
}
