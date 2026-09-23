package com.victorqueiroga.fastnotify.origem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DestinosStore {
    public static final int DEFAULT_PORT = 9876;
    public static final int DEFAULT_SCREEN = 0;
    public static final long DEFAULT_DURATION_MS = 5000;

    private final Path file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public DestinosStore(Path file) {
        this.file = file;
        ensureReady();
    }

    public Path getFile() {
        return file;
    }

    public List<Destino> load() {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            if (json.isBlank()) {
                return new ArrayList<>();
            }
            DestinoFile data = gson.fromJson(json, DestinoFile.class);
            if (data == null || data.destinos == null) {
                return new ArrayList<>();
            }
            List<Destino> out = new ArrayList<>();
            for (Destino d : data.destinos) {
                if (d != null && d.host() != null && !d.host().isBlank()) {
                    out.add(d.normalized());
                }
            }
            return out;
        } catch (Exception e) {
            System.err.println("Falha ao ler destinos: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void save(List<Destino> destinos) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            DestinoFile data = new DestinoFile();
            data.destinos = new ArrayList<>(destinos);
            Files.writeString(file, gson.toJson(data) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Falha ao salvar destinos: " + e.getMessage());
        }
    }

    private void ensureReady() {
        if (Files.exists(file)) {
            return;
        }
        save(new ArrayList<>());
    }

    public record Destino(String alias, String host, int port,
                          String token, Integer screen, Long durationMs) {

        public Destino normalized() {
            String a = alias == null || alias.isBlank() ? host : alias.trim();
            String h = host == null ? "" : host.trim();
            String t = token == null ? "" : token.trim();
            int p = port > 0 && port <= 65535 ? port : DEFAULT_PORT;
            int s = screen == null || screen < 0 ? DEFAULT_SCREEN : screen;
            long d = durationMs == null || durationMs <= 0 ? DEFAULT_DURATION_MS : durationMs;
            return new Destino(a, h, p, t, s, d);
        }

        public String display() {
            StringBuilder sb = new StringBuilder();
            sb.append(alias == null || alias.isBlank() ? host : alias);
            sb.append("  [").append(host).append(':').append(port);
            sb.append(" · tela ").append(effectiveScreen());
            sb.append(" · ").append(effectiveDurationMs()).append("ms");
            sb.append(token == null || token.isBlank() ? " · sem token" : " · token");
            sb.append(']');
            return sb.toString();
        }

        public String effectivePort() {
            return String.valueOf(port > 0 && port <= 65535 ? port : DEFAULT_PORT);
        }

        public int effectivePortInt() {
            return port > 0 && port <= 65535 ? port : DEFAULT_PORT;
        }

        public String effectiveToken() {
            return token == null ? "" : token;
        }

        public int effectiveScreen() {
            return screen == null || screen < 0 ? DEFAULT_SCREEN : screen;
        }

        public long effectiveDurationMs() {
            return durationMs == null || durationMs <= 0 ? DEFAULT_DURATION_MS : durationMs;
        }
    }

    private static final class DestinoFile {
        List<Destino> destinos = new ArrayList<>();
    }
}
