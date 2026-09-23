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
                if (d != null) {
                    Destino n = d.normalized();
                    if (!n.host().isBlank() || !n.effectiveHostIp().isBlank()) {
                        out.add(n);
                    }
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

    public boolean upsertByHost(String hostName, String hostIp, Destino nuevo) {
        List<Destino> list = load();
        String keyName = hostName == null ? "" : hostName.trim();
        String keyIp = hostIp == null ? "" : hostIp.trim();
        for (int i = 0; i < list.size(); i++) {
            Destino actual = list.get(i);
            if (matchesHost(actual, keyName, keyIp)) {
                list.set(i, nuevo.normalized());
                save(list);
                return false;
            }
        }
        list.add(nuevo.normalized());
        save(list);
        return true;
    }

    private static boolean matchesHost(Destino d, String hostName, String hostIp) {
        String h = d.host() == null ? "" : d.host().trim();
        String ip = d.effectiveHostIp();
        if (!hostName.isEmpty() && hostName.equalsIgnoreCase(h)) {
            return true;
        }
        if (!hostIp.isEmpty() && hostIp.equalsIgnoreCase(h)) {
            return true;
        }
        if (!hostIp.isEmpty() && hostIp.equalsIgnoreCase(ip)) {
            return true;
        }
        if (!hostName.isEmpty() && !ip.isEmpty() && hostName.equalsIgnoreCase(ip)) {
            return true;
        }
        return false;
    }

    public record Destino(String alias, String host, int port,
                          String token, Integer screen, Long durationMs, String department,
                          String hostIp) {

        public Destino(String alias, String host, int port,
                       String token, Integer screen, Long durationMs, String department) {
            this(alias, host, port, token, screen, durationMs, department, "");
        }

        public Destino normalized() {
            String a = alias == null || alias.isBlank() ? host() : alias.trim();
            String h = host == null ? "" : host.trim();
            String hip = hostIp == null ? "" : hostIp.trim();
            String t = token == null ? "" : token.trim();
            String d = department == null ? "" : department.trim();
            if (h.isBlank() && !hip.isBlank()) {
                h = hip;
            }
            if (!h.isBlank() && hip.isBlank() && looksLikeIp(h)) {
                hip = h;
            }
            int p = port > 0 && port <= 65535 ? port : DEFAULT_PORT;
            int s = screen == null || screen < 0 ? DEFAULT_SCREEN : screen;
            long ms = durationMs == null || durationMs <= 0 ? DEFAULT_DURATION_MS : durationMs;
            return new Destino(a.isBlank() ? h : a, h, p, t, s, ms, d, hip);
        }

        public String connectHost() {
            String h = host == null ? "" : host.trim();
            if (!h.isEmpty()) {
                return h;
            }
            return effectiveHostIp();
        }

        public String display() {
            StringBuilder sb = new StringBuilder();
            String target = connectHost();
            sb.append(alias == null || alias.isBlank() ? target : alias);
            if (department != null && !department.isBlank()) {
                sb.append("  [").append(department).append(']');
            }
            sb.append("  [").append(target).append(':').append(port);
            String ip = effectiveHostIp();
            if (!ip.isEmpty() && !ip.equalsIgnoreCase(target)) {
                sb.append(" · ").append(ip);
            }
            sb.append(" · tela ").append(effectiveScreen());
            sb.append(" · ").append(effectiveDurationMs()).append("ms");
            sb.append(token == null || token.isBlank() ? " · sem token" : " · token");
            sb.append(']');
            return sb.toString();
        }

        public String effectiveHostIp() {
            String ip = hostIp == null ? "" : hostIp.trim();
            if (!ip.isEmpty()) {
                return ip;
            }
            String h = host == null ? "" : host.trim();
            return looksLikeIp(h) ? h : "";
        }

        private static boolean looksLikeIp(String s) {
            return s != null && !s.isBlank()
                    && s.chars().allMatch(c -> (c >= '0' && c <= '9') || c == '.');
        }

        public String effectiveDepartment() {
            return department == null ? "" : department.trim();
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
