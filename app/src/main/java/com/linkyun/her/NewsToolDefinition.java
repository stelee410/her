package com.linkyun.her;

final class NewsToolDefinition implements ToolDefinition {
    static final String ID = "news";
    private static final String BACKGROUND_TOOL_ID = "daily_news";

    @Override public String id() {
        return ID;
    }

    @Override public boolean matches(String text) {
        return NewsSkill.isNewsQuestion(text);
    }

    @Override public boolean matchesBackgroundTool(String toolId) {
        return BACKGROUND_TOOL_ID.equals(toolId);
    }

    @Override public boolean start(String question, boolean realtimeMode, ToolRouter.Host host) {
        host.logToolRoute("news intent hit realtime=" + realtimeMode + " text=" + question);
        host.startNews(question, realtimeMode);
        return true;
    }

    @Override public boolean startFromBackground(String question, ToolRouter.Host host) {
        host.logToolRoute("news background intent hit text=" + question);
        host.startNewsFromBackground(question);
        return true;
    }
}
