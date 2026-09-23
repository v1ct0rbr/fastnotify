package com.victorqueiroga.fastnotify.destino;

public enum LogType {
    MESSAGE("Mensagem"),
    TEST_OK("Teste OK"),
    TEST_FAIL("Teste falha"),
    BLOCK("Bloqueio"),
    REJECT("Rejeição"),
    AUTH_OK("Token válido"),
    ERROR("Erro"),
    INFO("Info");

    private final String label;

    LogType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public String code() {
        return name();
    }

    public static LogType fromCode(String code) {
        for (LogType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return INFO;
    }
}
