package com.linkyun.her;

final class ToolSession {
    private static final ToolSession NONE = new ToolSession("", "", false, 0);

    private final String toolId;
    private final String question;
    private final boolean realtimeMode;
    private final int token;

    private ToolSession(String toolId, String question, boolean realtimeMode, int token) {
        this.toolId = toolId == null ? "" : toolId;
        this.question = question == null ? "" : question;
        this.realtimeMode = realtimeMode;
        this.token = token;
    }

    static ToolSession none() {
        return NONE;
    }

    static ToolSession start(String toolId, String question, boolean realtimeMode, int token) {
        return new ToolSession(toolId, question, realtimeMode, token);
    }

    String question() {
        return question;
    }

    boolean realtimeMode() {
        return realtimeMode;
    }

    int token() {
        return token;
    }

    String toolId() {
        return toolId;
    }

    boolean isRealtimeTool(String expectedToolId) {
        return realtimeMode && isTool(expectedToolId);
    }

    boolean matches(String expectedToolId, int callbackToken) {
        return token == callbackToken && isTool(expectedToolId);
    }

    private boolean isTool(String expectedToolId) {
        return !toolId.isEmpty() && toolId.equals(expectedToolId);
    }
}
