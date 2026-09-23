package com.victorqueiroga.fastnotify.origem;

public class NotificationMessage {
    private String token;
    private MsgType type;
    private long durationMs;
    private int screenIndex;
    private String title;
    private String body;
    private boolean test;
    private String senderFullName;
    private String senderDomain;

    public NotificationMessage() {
        this.type = MsgType.NOTIFICATION;
        this.durationMs = 5000;
        this.screenIndex = 0;
        this.title = "";
        this.body = "";
        this.senderFullName = "";
        this.senderDomain = "";
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public MsgType getType() {
        return type;
    }

    public void setType(MsgType type) {
        this.type = type;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getScreenIndex() {
        return screenIndex;
    }

    public void setScreenIndex(int screenIndex) {
        this.screenIndex = screenIndex;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body == null ? "" : body;
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String getSenderFullName() {
        return senderFullName;
    }

    public void setSenderFullName(String senderFullName) {
        this.senderFullName = senderFullName == null ? "" : senderFullName;
    }

    public String getSenderDomain() {
        return senderDomain;
    }

    public void setSenderDomain(String senderDomain) {
        this.senderDomain = senderDomain == null ? "" : senderDomain;
    }
}
