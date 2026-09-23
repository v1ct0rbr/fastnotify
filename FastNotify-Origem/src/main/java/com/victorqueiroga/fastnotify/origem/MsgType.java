package com.victorqueiroga.fastnotify.origem;

import javafx.scene.paint.Color;

public enum MsgType {
    ALERT("Alerta", Color.web("#DC2626"), Color.web("#FEF2F2"), "!"),
    NOTIFICATION("Notificação", Color.web("#2563EB"), Color.web("#EFF6FF"), "i"),
    INFO("Informação", Color.web("#059669"), Color.web("#ECFDF5"), "i");

    private final String label;
    private final Color accent;
    private final Color background;
    private final String badge;

    MsgType(String label, Color accent, Color background, String badge) {
        this.label = label;
        this.accent = accent;
        this.background = background;
        this.badge = badge;
    }

    public String label() {
        return label;
    }

    public Color accent() {
        return accent;
    }

    public Color background() {
        return background;
    }

    public String badge() {
        return badge;
    }

    public String accentCss() {
        return toCss(accent);
    }

    public String backgroundCss() {
        return toCss(background);
    }

    private static String toCss(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }

    public static MsgType fromCode(String code) {
        for (MsgType t : values()) {
            if (t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        return INFO;
    }
}
