package com.victorqueiroga.fastnotify.origem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MessageStore {
    private static final String CLASSPATH_DEFAULT = "/config/messages.properties";

    private final Path file;
    private final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    public MessageStore(Path file, ConfigStore config) {
        this.file = file;
        ensureReady(config);
    }

    public Path getFile() {
        return file;
    }

    public List<ConfigStore.FixedMessage> load() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return new ArrayList<>();
            }
            MessageFile data = gson.fromJson(json, MessageFile.class);
            if (data == null || data.messages == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(data.messages);
        } catch (Exception e) {
            System.err.println("Falha ao ler mensagens: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<ConfigStore.FixedMessage> messages) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            MessageFile data = new MessageFile();
            data.messages = new ArrayList<>(messages);
            Files.writeString(file, gson.toJson(data) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Falha ao salvar mensagens: " + e.getMessage());
        }
    }

    private void ensureReady(ConfigStore config) {
        if (Files.exists(file)) {
            return;
        }
        List<ConfigStore.FixedMessage> legacy = config.getFixedMessages();
        if (!legacy.isEmpty()) {
            save(legacy);
            config.clearFixedMessages();
            config.save();
            return;
        }
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (InputStream in = MessageStore.class.getResourceAsStream(CLASSPATH_DEFAULT)) {
                if (in != null) {
                    Files.copy(in, file);
                    return;
                }
            }
            save(new ArrayList<>());
        } catch (IOException e) {
            System.err.println("Falha ao criar messages.properties: " + e.getMessage());
        }
    }

    private static final class MessageFile {
        List<ConfigStore.FixedMessage> messages = new ArrayList<>();
    }
}
