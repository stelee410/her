package com.linkyun.her;

enum VoiceSessionStatus {
    IDLE("idle"),
    CONNECTING("connecting"),
    READY("ready"),
    LISTENING("listening"),
    THINKING("thinking"),
    PROCESSING("processing"),
    SPEAKING("speaking"),
    NEWS_ACK("news_ack"),
    NEWS_TOOL("news_tool"),
    WEATHER_TOOL("weather_tool"),
    APP_RUNNING("app_running"),
    SUMMARIZING("summarizing"),
    TEXT_ONLY("text_only"),
    ERROR("error"),
    UNKNOWN("");

    private final String legacyValue;

    VoiceSessionStatus(String legacyValue) {
        this.legacyValue = legacyValue;
    }

    String legacyValue() {
        return legacyValue;
    }

    static VoiceSessionStatus fromLegacy(String value) {
        String normalized = normalize(value);
        for (VoiceSessionStatus status : values()) {
            if (status != UNKNOWN && status.legacyValue.equals(normalized)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    static String normalize(String value) {
        if (value == null) return IDLE.legacyValue;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return IDLE.legacyValue;
        return trimmed.toLowerCase(java.util.Locale.US);
    }
}
