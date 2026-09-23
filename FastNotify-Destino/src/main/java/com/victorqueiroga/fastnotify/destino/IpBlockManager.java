package com.victorqueiroga.fastnotify.destino;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IpBlockManager {
    public static final int ATTEMPTS_THRESHOLD = 3;
    public static final long BASE_BLOCK_SECONDS = 30;
    public static final long STEP_SECONDS = 30;

    private static final class State {
        int invalidAttempts;
        int blockLevel;
        long blockedUntilMs;
    }

    private final Map<String, State> states = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        State s = states.get(ip);
        if (s == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= s.blockedUntilMs) {
            s.blockedUntilMs = 0;
            return false;
        }
        return true;
    }

    public long remainingBlockMs(String ip) {
        State s = states.get(ip);
        if (s == null) {
            return 0;
        }
        long rem = s.blockedUntilMs - System.currentTimeMillis();
        return Math.max(0, rem);
    }

    public int currentBlockLevel(String ip) {
        State s = states.get(ip);
        return s == null ? 0 : s.blockLevel;
    }

    public int invalidAttempts(String ip) {
        State s = states.get(ip);
        return s == null ? 0 : s.invalidAttempts;
    }

    public void onSuccess(String ip) {
        states.remove(ip);
    }

    /**
     * Registra tentativa inválida. Retorna os segundos de bloqueio aplicados
     * (0 se ainda não atingiu o limite de 3 tentativas).
     */
    public long onFailure(String ip) {
        if (isBlocked(ip)) {
            return 0;
        }
        State s = states.computeIfAbsent(ip, k -> new State());
        s.invalidAttempts++;
        if (s.invalidAttempts < ATTEMPTS_THRESHOLD) {
            return 0;
        }
        s.blockLevel++;
        long blockSeconds = BASE_BLOCK_SECONDS + (long) (s.blockLevel - 1) * STEP_SECONDS;
        s.blockedUntilMs = System.currentTimeMillis() + blockSeconds * 1000L;
        s.invalidAttempts = 0;
        return blockSeconds;
    }

    public String blockReason(String ip) {
        long remMs = remainingBlockMs(ip);
        long remSec = (remMs + 999) / 1000;
        return "IP bloqueado por " + remSec + "s (nível " + currentBlockLevel(ip) + ")";
    }

    public void resetAll() {
        states.clear();
    }
}
