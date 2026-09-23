package com.victorqueiroga.fastnotify.origem;

public enum LogType {
    SEND("Envio"),
    TEST_OK("Teste OK"),
    TEST_FAIL("Teste falha"),
    REGISTER("Cadastro"),
    FIREWALL("Firewall"),
    CONFIG("Config"),
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
