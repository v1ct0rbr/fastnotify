package com.victorqueiroga.fastnotify.origem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigStore {
    private static final String CLASSPATH_DEFAULT = "/config/origem.properties";

    private final Path file;
    private final Properties props = new Properties();

    public ConfigStore(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        props.clear();
        try (InputStream in = ConfigStore.class.getResourceAsStream(CLASSPATH_DEFAULT)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("Falha ao ler config padrão (jar): " + e.getMessage());
        }

        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            } catch (IOException e) {
                System.err.println("Falha ao ler config: " + e.getMessage());
            }
        } else {
            save();
        }
    }

    public void save() {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "FastNotify - Origem");
            }
        } catch (IOException e) {
            System.err.println("Falha ao salvar config: " + e.getMessage());
        }
    }

    public Path getFile() {
        return file;
    }

    public String getHost() {
        return props.getProperty("host", "127.0.0.1");
    }

    public void setHost(String host) {
        props.setProperty("host", host);
    }

    public int getPort() {
        return Integer.parseInt(props.getProperty("port", "9876"));
    }

    public void setPort(int port) {
        props.setProperty("port", String.valueOf(port));
    }

    public int getScreenIndex() {
        return Integer.parseInt(props.getProperty("screen", "0"));
    }

    public void setScreenIndex(int screen) {
        props.setProperty("screen", String.valueOf(screen));
    }

    public long getDurationMs() {
        return Long.parseLong(props.getProperty("durationMs", "5000"));
    }

    public void setDurationMs(long durationMs) {
        props.setProperty("durationMs", String.valueOf(durationMs));
    }

    public String getToken() {
        return props.getProperty("token", "");
    }

    public void setToken(String token) {
        props.setProperty("token", token == null ? "" : token);
    }

    public List<FixedMessage> getFixedMessages() {
        List<FixedMessage> list = new ArrayList<>();
        int count = Integer.parseInt(props.getProperty("fixed.count", "0"));
        for (int i = 0; i < count; i++) {
            String type = props.getProperty("fixed." + i + ".type", "NOTIFICATION");
            String title = props.getProperty("fixed." + i + ".title", "");
            String body = props.getProperty("fixed." + i + ".body", "");
            list.add(new FixedMessage(MsgType.fromCode(type), title, body));
        }
        return list;
    }

    public void clearFixedMessages() {
        for (String key : new ArrayList<>(props.stringPropertyNames())) {
            if (key.startsWith("fixed.")) {
                props.remove(key);
            }
        }
        props.setProperty("fixed.count", "0");
    }

    public static class FixedMessage {
        private final MsgType type;
        private final String title;
        private final String body;

        public FixedMessage(MsgType type, String title, String body) {
            this.type = type;
            this.title = title;
            this.body = body;
        }

        public MsgType type() {
            return type;
        }

        public String title() {
            return title;
        }

        public String body() {
            return body;
        }

        @Override
        public String toString() {
            return "[" + type.label() + "] " + title;
        }
    }
}
