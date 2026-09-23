package com.victorqueiroga.fastnotify.destino;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class UserNames {
    private static volatile String fullName = "";
    private static volatile String realFullName = "";
    private static volatile String domainOrGroup = "";
    private static volatile boolean resolved;

    private UserNames() {
    }

    public static String fullName() {
        ensureResolved();
        return fullName;
    }

    public static String realFullName() {
        ensureResolved();
        return realFullName;
    }

    public static String domainOrGroup() {
        ensureResolved();
        return domainOrGroup;
    }

    public static void warmUp() {
        ensureResolved();
    }

    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        synchronized (UserNames.class) {
            if (resolved) {
                return;
            }
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String user = System.getProperty("user.name", "");
            String resolvedName;
            if (os.contains("win")) {
                resolvedName = sanitize(resolveWindowsFullName());
                domainOrGroup = sanitize(resolveWindowsDomainOrGroup());
            } else if (os.contains("mac") || os.contains("darwin")) {
                resolvedName = sanitize(resolveMacFullName());
                domainOrGroup = sanitize(resolveUnixDomainOrGroup());
            } else {
                resolvedName = sanitize(resolveUnixFullName());
                domainOrGroup = sanitize(resolveUnixDomainOrGroup());
            }
            realFullName = resolvedName.trim();
            fullName = firstNonBlank(resolvedName, user);
            domainOrGroup = domainOrGroup.trim();
            resolved = true;
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        String s = value.replace('\uFFFD', ' ').replace('\0', ' ').trim();
        if (s.isEmpty() || looksLikePowerShellError(s)) {
            return "";
        }
        return s;
    }

    private static boolean looksLikePowerShellError(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("categoryinfo")
                || l.contains("fullyqualifiederrorid")
                || l.contains("objectnotfound")
                || l.contains("get-localuser")
                || l.contains("get-ciminstance")
                || l.contains("at line:")
                || l.contains("at linha")
                || l.contains("não encontrado")
                || l.contains("nao encontrado")
                || l.contains("not recognized")
                || l.contains("not found")
                || l.contains("term ")
                || (s.contains("+ ") && s.contains("~~~~"))
                || l.startsWith("get-")
                || l.contains("microsoft.powershell");
    }

    private static String resolveWindowsFullName() {
        String s = psValue(
                "try { $x=(Get-LocalUser -Name $env:USERNAME -EA Stop).FullName;"
                        + " if($x){$x} } catch {}");
        if (!s.isEmpty()) {
            return s;
        }
        s = psValue(
                "try { $x=([adsisearcher]\"(&(objectCategory=person)"
                        + "(objectClass=user)(sAMAccountName=$env:USERNAME))\")"
                        + ".FindOne().Properties.displayname;"
                        + " if($x){$x[0]} } catch {}");
        if (!s.isEmpty()) {
            return s;
        }
        s = psValue(
                "try { $p='HKCU:\\Software\\Microsoft\\Office\\Common\\UserInfo';"
                        + " $x=(Get-ItemProperty $p -EA Stop).UserName;"
                        + " if($x){$x} } catch {}");
        if (!s.isEmpty()) {
            return s;
        }
        return "";
    }

    private static String resolveWindowsDomainOrGroup() {
        String who = psValue(
                "try { [Security.Principal.WindowsIdentity]::GetCurrent().Name } catch {}");
        String computer = System.getenv("COMPUTERNAME");
        String userDomain = System.getenv("USERDOMAIN");
        if (userDomain == null || userDomain.isBlank()) {
            userDomain = System.getenv("USERDNSDOMAIN");
        }

        if (who.contains("\\")) {
            String d = who.substring(0, who.indexOf('\\'));
            String u = who.substring(who.indexOf('\\') + 1);
            if (computer == null || !d.equalsIgnoreCase(computer)) {
                return d + "\\" + u;
            }
            if (userDomain != null && !userDomain.isBlank()
                    && !userDomain.equalsIgnoreCase(computer)) {
                return userDomain + "\\" + u;
            }
        }

        String group = psValue(
                "try { $g=[Security.Principal.WindowsIdentity]::GetCurrent().Groups"
                        + " | ForEach-Object { try { $_.Translate([string]) } catch {} }"
                        + " | Where-Object { $_ -and $_ -notmatch '^(NT AUTHORITY|BUILTIN|"
                        + "APPLICATION PACKAGE|MANDATORY LABEL|CONSOLE LOGON|INTERACTIVE|"
                        + "NETWORK|REMOTE|THIS ORGANIZATION|AUTHENTICATED|EVERYONE|OWNER)' }"
                        + " | Select-Object -First 1; if($g){$g} } catch {}");
        if (!group.isEmpty()) {
            return group;
        }
        if (userDomain != null && !userDomain.isBlank()
                && (computer == null || !userDomain.equalsIgnoreCase(computer))) {
            String u = System.getProperty("user.name", "");
            return u.isEmpty() ? userDomain : userDomain + "\\" + u;
        }
        if (who.contains("\\")) {
            return who;
        }
        return "";
    }

    private static String resolveMacFullName() {
        String user = System.getProperty("user.name", "");
        if (user.isEmpty()) {
            return "";
        }
        String s = execCapture("dscl", ".", "-read", "/Users/" + user, "RealName");
        if (!s.isEmpty()) {
            int nl = s.indexOf('\n');
            return (nl > 0 ? s.substring(0, nl) : s).trim();
        }
        return execCapture("id", "-Fn");
    }

    private static String resolveUnixFullName() {
        String user = System.getProperty("user.name", "");
        if (!user.isEmpty()) {
            String gecos = fromPasswd(user);
            if (!gecos.isEmpty()) {
                return gecos;
            }
        }
        String s = execCapture("getent", "passwd", user);
        if (!s.isEmpty()) {
            String[] p = s.split(":");
            if (p.length > 4) {
                String gecos = p[4];
                int comma = gecos.indexOf(',');
                if (comma > 0) {
                    gecos = gecos.substring(0, comma);
                }
                if (!gecos.isBlank()) {
                    return gecos.trim();
                }
            }
        }
        String id = execCapture("id", "-Fn");
        if (!id.isEmpty() && !id.equals(user)) {
            return id;
        }
        return "";
    }

    private static String resolveUnixDomainOrGroup() {
        String primary = execCapture("id", "-gn");
        String dns = execCapture("dnsdomainname");
        String user = System.getProperty("user.name", "");
        if (!dns.isEmpty() && !dns.equals("(none)")) {
            return user.isEmpty() ? dns : dns + "\\" + user;
        }
        if (!primary.isEmpty() && !primary.equals(user)
                && !"users".equalsIgnoreCase(primary)) {
            return primary;
        }
        String groups = execCapture("id", "-nG");
        if (!groups.isEmpty()) {
            for (String g : groups.split("\\s+")) {
                if (!g.equalsIgnoreCase(user) && !"users".equalsIgnoreCase(g)) {
                    return g;
                }
            }
        }
        return "";
    }

    private static String fromPasswd(String user) {
        try {
            for (String line : Files.readAllLines(Path.of("/etc/passwd"))) {
                if (line.startsWith(user + ":")) {
                    String[] p = line.split(":");
                    if (p.length > 4) {
                        String gecos = p[4];
                        int comma = gecos.indexOf(',');
                        if (comma > 0) {
                            gecos = gecos.substring(0, comma);
                        }
                        if (!gecos.isBlank()) {
                            return gecos.trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static String psValue(String command) {
        try {
            String script = "[Console]::OutputEncoding=[System.Text.UTF8Encoding]::new($false); "
                    + command;
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-Command", script);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            String out = readAll(p, 4);
            if (!p.waitFor(4, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "";
            }
            if (p.exitValue() != 0) {
                return "";
            }
            return out.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String execCapture(String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            String out = readAll(p, 3);
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return "";
            }
            if (p.exitValue() != 0) {
                return "";
            }
            return out.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String readAll(Process p, int seconds) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (!t.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }
}
