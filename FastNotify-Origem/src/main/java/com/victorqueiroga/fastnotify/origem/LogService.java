package com.victorqueiroga.fastnotify.origem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LogService implements AutoCloseable {
    public static final long FLUSH_INTERVAL_MINUTES = 10;
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final java.util.regex.Pattern LOG_FILE =
            java.util.regex.Pattern.compile("^logs-(\\d{4}-\\d{2}-\\d{2})\\.log$");

    public record LogEntry(LocalDateTime time, LogType type, String ip, String user,
                           String message) {
        public String toLine() {
            String u = user == null || user.isBlank() ? "-" : user;
            return TS.format(time) + " | " + type.code() + " | " + ip + " | " + u
                    + " | " + message;
        }

        public LocalDate date() {
            return time.toLocalDate();
        }
    }

    private final Path logsDir;
    private volatile int retentionDays;
    private final ConcurrentLinkedQueue<LogEntry> cache = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "fastnotify-origem-log-flush");
                t.setDaemon(true);
                return t;
            });
    private volatile boolean closed;

    public LogService(Path logsDir) {
        this(logsDir, 30);
    }

    public LogService(Path logsDir, int retentionDays) {
        this.logsDir = logsDir;
        this.retentionDays = Math.max(0, retentionDays);
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            System.err.println("Falha ao criar pasta de logs: " + e.getMessage());
        }
        purgeExpired();
        scheduler.scheduleAtFixedRate(this::flushIfNotEmpty,
                FLUSH_INTERVAL_MINUTES, FLUSH_INTERVAL_MINUTES, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(this::purgeExpired,
                FLUSH_INTERVAL_MINUTES, FLUSH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public Path getLogsDir() {
        return logsDir;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public int setRetentionDays(int days) {
        this.retentionDays = Math.max(0, days);
        return purgeExpired();
    }

    public int purgeExpired() {
        int days = retentionDays;
        if (days <= 0) {
            return 0;
        }
        LocalDate cutoff = LocalDate.now().minusDays(days);
        if (!Files.isDirectory(logsDir)) {
            return 0;
        }
        int deleted = 0;
        try (var stream = Files.list(logsDir)) {
            for (Path p : stream.toList()) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                java.util.regex.Matcher m =
                        LOG_FILE.matcher(p.getFileName().toString());
                if (!m.matches()) {
                    continue;
                }
                LocalDate fileDate;
                try {
                    fileDate = LocalDate.parse(m.group(1), FILE_DATE);
                } catch (java.time.format.DateTimeParseException e) {
                    continue;
                }
                if (fileDate.isBefore(cutoff)) {
                    try {
                        Files.deleteIfExists(p);
                        deleted++;
                    } catch (IOException e) {
                        System.err.println("Falha ao excluir log expirado "
                                + p.getFileName() + ": " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Falha ao varrer pasta de logs: " + e.getMessage());
        }
        return deleted;
    }

    public void append(LogType type, String ip, String message) {
        if (closed) {
            return;
        }
        cache.add(new LogEntry(LocalDateTime.now(), type,
                ip == null || ip.isBlank() ? "-" : ip,
                userWithDomain(),
                message == null ? "" : message.replace('\n', ' ').replace('\r', ' ')));
    }

    public int cacheSize() {
        return cache.size();
    }

    private static String userWithDomain() {
        String name = UserNames.fullName();
        String domain = UserNames.domainOrGroup();
        if (domain != null && !domain.isBlank()) {
            return name + " (" + domain + ")";
        }
        return name;
    }

    public synchronized int flush() {
        if (cache.isEmpty()) {
            return 0;
        }
        List<LogEntry> batch = new ArrayList<>(cache);
        Map<LocalDate, List<LogEntry>> byDate = batch.stream()
                .collect(Collectors.groupingBy(LogEntry::date));

        int written = 0;
        for (Map.Entry<LocalDate, List<LogEntry>> e : byDate.entrySet()) {
            Path file = logFile(e.getKey());
            try {
                if (file.getParent() != null) {
                    Files.createDirectories(file.getParent());
                }
                List<String> lines = e.getValue().stream().map(LogEntry::toLine).toList();
                Files.write(file, lines, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                written += lines.size();
            } catch (IOException ex) {
                System.err.println("Falha ao gravar log: " + ex.getMessage());
            }
        }
        cache.removeAll(batch);
        return written;
    }

    private void flushIfNotEmpty() {
        if (!cache.isEmpty()) {
            flush();
        }
    }

    public Path logFile(LocalDate date) {
        return logsDir.resolve("logs-" + date.format(FILE_DATE) + ".log");
    }

    public List<String> readLines(LocalDate date, LogType filter) {
        List<String> result = new ArrayList<>();
        Path file = logFile(date);
        if (Files.exists(file)) {
            try {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    if (matches(line, filter)) {
                        result.add(line);
                    }
                }
            } catch (IOException e) {
                result.add("Erro ao ler " + file + ": " + e.getMessage());
            }
        }
        for (LogEntry entry : cache) {
            if (entry.date().equals(date)) {
                String line = entry.toLine();
                if (matches(line, filter)) {
                    result.add(line);
                }
            }
        }
        return result;
    }

    private static boolean matches(String line, LogType filter) {
        if (filter == null) {
            return true;
        }
        int first = line.indexOf(" | ");
        if (first < 0) {
            return false;
        }
        int second = line.indexOf(" | ", first + 3);
        if (second < 0) {
            return false;
        }
        String code = line.substring(first + 3, second);
        return filter.name().equalsIgnoreCase(code);
    }

    @Override
    public void close() {
        closed = true;
        scheduler.shutdownNow();
        flush();
    }
}
