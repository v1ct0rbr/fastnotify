package com.victorqueiroga.fastnotify.destino;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class LocalHostInfo {
    private static volatile String hostname = "";
    private static volatile String primaryIp = "";
    private static volatile List<String> ips = List.of();
    private static volatile boolean resolved;

    private LocalHostInfo() {
    }

    public static String hostname() {
        ensureResolved();
        return hostname;
    }

    public static String primaryIp() {
        ensureResolved();
        return primaryIp;
    }

    public static List<String> allIps() {
        ensureResolved();
        return ips;
    }

    public static String display() {
        ensureResolved();
        if (primaryIp.isEmpty()) {
            return hostname;
        }
        if (hostname.isEmpty()) {
            return primaryIp;
        }
        return hostname + "  ·  " + primaryIp;
    }

    public static void warmUp() {
        ensureResolved();
    }

    private static void ensureResolved() {
        if (resolved) {
            return;
        }
        synchronized (LocalHostInfo.class) {
            if (resolved) {
                return;
            }
            String h = "";
            try {
                h = InetAddress.getLocalHost().getHostName();
            } catch (Exception ignored) {
            }
            if (h == null || h.isBlank()) {
                h = System.getenv("COMPUTERNAME");
            }
            if (h == null || h.isBlank()) {
                h = System.getenv("HOSTNAME");
            }
            hostname = h == null ? "" : h.trim();

            List<String> found = new ArrayList<>();
            String fallback = "";
            try {
                Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
                if (nifs != null) {
                    for (NetworkInterface nif : Collections.list(nifs)) {
                        if (nif == null || !nif.isUp() || nif.isLoopback()
                                || nif.isVirtual()) {
                            continue;
                        }
                        for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                            if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                                String ip = addr.getHostAddress();
                                if (!found.contains(ip)) {
                                    found.add(ip);
                                }
                                if (fallback.isEmpty()) {
                                    fallback = ip;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            if (fallback.isEmpty()) {
                try {
                    InetAddress local = InetAddress.getLocalHost();
                    if (!local.isLoopbackAddress()) {
                        fallback = local.getHostAddress();
                        if (fallback != null && !fallback.contains(":")
                                && !found.contains(fallback)) {
                            found.add(0, fallback);
                        } else {
                            fallback = found.isEmpty() ? "" : found.get(0);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            if (fallback.isEmpty() && !found.isEmpty()) {
                fallback = found.get(0);
            }
            primaryIp = fallback;
            ips = List.copyOf(found);
            resolved = true;
        }
    }
}
