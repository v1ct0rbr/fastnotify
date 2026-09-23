package com.victorqueiroga.fastnotify.destino;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigStore {
    private static final String CLASSPATH_DEFAULT = "/config/destino.properties";

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
                props.store(out, "FastNotify - Destino");
            }
        } catch (IOException e) {
            System.err.println("Falha ao salvar config: " + e.getMessage());
        }
    }

    public Path getFile() {
        return file;
    }

    public int getPort() {
        return Integer.parseInt(props.getProperty("port", "9876"));
    }

    public void setPort(int port) {
        props.setProperty("port", String.valueOf(port));
    }

    public String getToken() {
        return props.getProperty("token", "");
    }

    public void setToken(String token) {
        props.setProperty("token", token == null ? "" : token);
    }

    public boolean isSoundEnabled() {
        return Boolean.parseBoolean(props.getProperty("soundEnabled", "true"));
    }

    public void setSoundEnabled(boolean enabled) {
        props.setProperty("soundEnabled", String.valueOf(enabled));
    }

    public String getOrigemHost() {
        return props.getProperty("origemHost", "");
    }

    public void setOrigemHost(String host) {
        props.setProperty("origemHost", host == null ? "" : host);
    }

    public int getOrigemRegisterPort() {
        try {
            return Integer.parseInt(props.getProperty("origemRegisterPort", "9877"));
        } catch (NumberFormatException e) {
            return 9877;
        }
    }

    public void setOrigemRegisterPort(int port) {
        props.setProperty("origemRegisterPort", String.valueOf(port));
    }

    public String getOrigemRegisterToken() {
        return props.getProperty("origemRegisterToken", "");
    }

    public void setOrigemRegisterToken(String token) {
        props.setProperty("origemRegisterToken", token == null ? "" : token);
    }

    public String getAlias() {
        return props.getProperty("alias", "");
    }

    public void setAlias(String alias) {
        props.setProperty("alias", alias == null ? "" : alias);
    }

    public String getDepartment() {
        return props.getProperty("department", "");
    }

    public void setDepartment(String department) {
        props.setProperty("department", department == null ? "" : department);
    }

    public String getPsk() {
        return props.getProperty("psk", "");
    }

    public void setPsk(String psk) {
        props.setProperty("psk", psk == null ? "" : psk);
    }

    public String effectivePsk(String token) {
        String psk = getPsk();
        if (psk != null && !psk.isEmpty()) {
            return psk;
        }
        return token == null ? "" : token;
    }

    public int getLogRetentionDays() {
        try {
            return Integer.parseInt(props.getProperty("logRetentionDays", "30"));
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    public void setLogRetentionDays(int days) {
        props.setProperty("logRetentionDays", String.valueOf(Math.max(0, days)));
    }
}
