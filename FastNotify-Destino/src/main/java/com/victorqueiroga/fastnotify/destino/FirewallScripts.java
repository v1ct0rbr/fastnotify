package com.victorqueiroga.fastnotify.destino;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class FirewallScripts {
    private FirewallScripts() {
    }

    public static void liberarPorta(Path configDir) {
        runElevated("liberar-porta-destino.ps1", "liberar-porta-destino.bat", configDir);
    }

    public static void removerPorta(Path configDir) {
        runElevated("liberar-porta-destino.ps1", "remover-porta-destino.bat", configDir, "-Acao", "Remove");
    }

    public static void instalarInicializacao() {
        runUser("instalar-inicializacao-destino.ps1", "instalar-inicializacao-destino.bat");
    }

    public static void removerInicializacao() {
        runUser("remover-inicializacao-destino.ps1", "remover-inicializacao-destino.bat");
    }

    private static void runUser(String ps1Name, String batName) {
        try {
            Path ps1 = resolveScript(ps1Name, batName);
            List<String> args = new ArrayList<>();
            args.add("cmd");
            args.add("/c");
            args.add("start");
            args.add("\"FastNotify\"");
            args.add("powershell");
            args.add("-NoProfile");
            args.add("-ExecutionPolicy");
            args.add("Bypass");
            args.add("-File");
            args.add(ps1.toAbsolutePath().toString());
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.directory(ps1.toAbsolutePath().getParent().toFile());
            pb.redirectErrorStream(true);
            pb.start();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Falha ao executar " + ps1Name + ": " + ex.getMessage(), ex);
        }
    }

    private static void runElevated(String ps1Name, String batName, Path configDir,
                                    String... extraArgs) {
        try {
            Path ps1 = resolveScript(ps1Name, batName);
            List<String> args = new ArrayList<>();
            args.add("powershell.exe");
            args.add("-NoProfile");
            args.add("-Command");
            StringBuilder cmd = new StringBuilder();
            cmd.append("Start-Process -FilePath powershell -Verb RunAs -ArgumentList ");
            appendArg(cmd, "-NoProfile");
            appendArg(cmd, "-ExecutionPolicy");
            appendArg(cmd, "Bypass");
            appendArg(cmd, "-File");
            appendArg(cmd, ps1.toAbsolutePath().toString());
            if (configDir != null) {
                appendArg(cmd, "-ConfigDir");
                appendArg(cmd, configDir.toAbsolutePath().toString());
            }
            for (String extra : extraArgs) {
                appendArg(cmd, extra);
            }
            cmd.append(" -WorkingDirectory ");
            appendArg(cmd, ps1.toAbsolutePath().getParent().toString());
            args.add(cmd.toString());
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            pb.start();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Falha ao executar " + batName + ": " + ex.getMessage(), ex);
        }
    }

    private static void appendArg(StringBuilder cmd, String value) {
        if (cmd.charAt(cmd.length() - 1) != ' ') {
            cmd.append(' ');
        }
        cmd.append('\'').append(value.replace("'", "''")).append('\'');
    }

    private static Path resolveScript(String ps1Name, String batName) throws IOException {
        for (Path dir : searchDirs()) {
            Path ps1 = dir.resolve(ps1Name);
            if (Files.isRegularFile(ps1)) {
                return ps1;
            }
            Path bat = dir.resolve(batName);
            if (Files.isRegularFile(bat)) {
                Path sibling = dir.resolve(ps1Name);
                if (Files.isRegularFile(sibling)) {
                    return sibling;
                }
            }
        }
        Path tempDir = Files.createTempDirectory("fastnotify-scripts");
        extract(ps1Name, tempDir.resolve(ps1Name));
        extract(batName, tempDir.resolve(batName));
        return tempDir.resolve(ps1Name);
    }

    private static List<Path> searchDirs() {
        List<Path> dirs = new ArrayList<>();
        Path cwd = Path.of("").toAbsolutePath();
        dirs.add(cwd);
        dirs.add(cwd.resolve("target"));
        dirs.add(cwd.resolve("src").resolve("main").resolve("resources"));
        try {
            Path code = Path.of(FirewallScripts.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
            if (Files.isRegularFile(code)) {
                Path parent = code.getParent();
                if (parent != null) {
                    dirs.add(parent);
                }
            } else if (Files.isDirectory(code)) {
                dirs.add(code);
            }
        } catch (Exception ignored) {
        }
        return dirs;
    }

    private static void extract(String name, Path target) throws IOException {
        try (InputStream in = FirewallScripts.class.getResourceAsStream("/" + name)) {
            if (in == null) {
                throw new IOException("Recurso nao encontrado no classpath: " + name);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
