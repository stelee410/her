package com.linkyun.her;

final class ToolRouter {
    interface Host {
        boolean hasLatestWeatherFact();
        void reuseLatestWeatherFact(String question);
        void startNews(String question, boolean realtimeMode);
        void startNewsFromBackground(String question);
        void startWeather(String question, boolean realtimeMode);
        void adjustVoiceVolume(VolumeSkill.Direction direction);
        void logToolRoute(String message);
    }

    private static final double BACKGROUND_CONFIDENCE_THRESHOLD = 0.55;

    private final ToolRegistry registry;
    private final Host host;

    ToolRouter(ToolRegistry registry, Host host) {
        this.registry = registry;
        this.host = host;
    }

    boolean routeUserText(String text, boolean realtimeMode) {
        String question = normalizeQuestion(text);
        if (question.isEmpty()) {
            host.logToolRoute("tool intent miss text=");
            return false;
        }
        ToolDefinition tool = registry.match(question);
        if (tool == null) {
            host.logToolRoute("tool intent miss text=" + question);
            return false;
        }
        return tool.start(question, realtimeMode, host);
    }

    boolean routeBackgroundDecision(String toolId, double confidence, String text) {
        if (confidence < BACKGROUND_CONFIDENCE_THRESHOLD) return false;
        String question = normalizeQuestion(text);
        if (question.isEmpty()) return false;
        ToolDefinition tool = registry.backgroundTool(toolId);
        if (tool == null) return false;
        return tool.startFromBackground(question, host);
    }

    private static String normalizeQuestion(String text) {
        return text == null ? "" : text.trim();
    }
}
