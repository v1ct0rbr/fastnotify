package com.victorqueiroga.fastnotify.destino;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class FirewallChecker {
    private static final int TIMEOUT_MS = 8000;

    private FirewallChecker() {
    }

    public static String checkInboundRule(String ruleName, int port) {
        if (!isWindows()) {
            return "Firewall: checagem so no Windows (porta " + port + ").";
        }
        String script = String.join("; ",
                "$ErrorActionPreference='SilentlyContinue'",
                "$r=Get-NetFirewallRule -Name '" + escape(ruleName) + "' -ErrorAction SilentlyContinue",
                "if(-not $r){Write-Output 'AUSENTE'}",
                "elseif($r.Enabled -eq 'True' -and [string]$r.Action -eq 'Allow'){Write-Output ('OK ' + " + port + ")}",
                "else{Write-Output ('FECHADA ' + [string]$r.Action + '/' + [string]$r.Enabled)}"
        );
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive", "-Command", script);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.isBlank()) {
                        out.append(line.trim());
                    }
                }
            }
            if (!p.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return "Firewall: timeout ao consultar porta " + port + ".";
            }
            String text = out.toString().trim();
            if (text.startsWith("OK")) {
                return "Firewall entrada " + port + ": liberada (regra ativa).";
            }
            if (text.startsWith("FECHADA")) {
                return "Firewall entrada " + port + ": regra existe mas esta "
                        + text.substring("FECHADA".length()).trim()
                        + " — rode liberar-porta-destino.bat.";
            }
            if (text.startsWith("AUSENTE")) {
                return "Firewall entrada " + port + ": sem regra FastNotify — "
                        + "rode liberar-porta-destino.bat (admin).";
            }
            return "Firewall entrada " + port + ": " + (text.isEmpty() ? "sem dados" : text) + ".";
        } catch (Exception e) {
            return "Firewall entrada " + port + ": falha ao consultar — " + e.getMessage();
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "");
        return os.toLowerCase().contains("win");
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("'", "''");
    }
}
