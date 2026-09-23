package com.victorqueiroga.fastnotify.destino;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class Protocol {
    public static final String MAGIC = "FASTNOTIFY/1";
    public static final String ACK_MAGIC = "FASTNOTIFY/ACK";
    public static final int VERSION = 2;

    private Protocol() {
    }

    public static void write(DataOutputStream out, NotificationMessage msg) throws IOException {
        out.writeUTF(MAGIC);
        out.writeInt(VERSION);
        out.writeUTF(msg.getToken() == null ? "" : msg.getToken());
        out.writeUTF(msg.getType().name());
        out.writeLong(msg.getDurationMs());
        out.writeInt(msg.getScreenIndex());
        out.writeBoolean(msg.isTest());
        out.writeUTF(msg.getTitle());
        out.writeUTF(msg.getBody());
        out.flush();
    }

    public static NotificationMessage read(DataInputStream in) throws IOException {
        String magic = in.readUTF();
        if (!MAGIC.equals(magic)) {
            throw new IOException("Protocolo inválido: " + magic);
        }
        int version = in.readInt();
        if (version != VERSION) {
            throw new IOException("Versão incompatível: " + version);
        }
        NotificationMessage msg = new NotificationMessage();
        msg.setToken(in.readUTF());
        msg.setType(MsgType.fromCode(in.readUTF()));
        msg.setDurationMs(in.readLong());
        msg.setScreenIndex(in.readInt());
        msg.setTest(in.readBoolean());
        msg.setTitle(in.readUTF());
        msg.setBody(in.readUTF());
        return msg;
    }

    public static void writeAck(DataOutputStream out, boolean ok, String detail) throws IOException {
        out.writeUTF(ACK_MAGIC);
        out.writeBoolean(ok);
        out.writeUTF(detail == null ? "" : detail);
        out.flush();
    }

    public static Ack readAck(DataInputStream in) throws IOException {
        String magic = in.readUTF();
        if (!ACK_MAGIC.equals(magic)) {
            throw new IOException("ACK inválido: " + magic);
        }
        boolean ok = in.readBoolean();
        String detail = in.readUTF();
        return new Ack(ok, detail);
    }

    public record Ack(boolean ok, String detail) {
    }
}
