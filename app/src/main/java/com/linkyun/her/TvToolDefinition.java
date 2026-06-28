package com.linkyun.her;

final class TvToolDefinition implements ToolDefinition {
    static final String ID = "tv";
    static final String LLM_TOOL_ID = "open_tv";

    @Override public String id() {
        return ID;
    }

    @Override public boolean matches(String text) {
        return TvVoiceCommand.shouldOpen(text);
    }

    @Override public boolean matchesBackgroundTool(String toolId) {
        return LLM_TOOL_ID.equals(toolId);
    }

    @Override public boolean start(String question, boolean realtimeMode, ToolRouter.Host host) {
        host.logToolRoute("tv intent hit realtime=" + realtimeMode + " text=" + question);
        host.openTv(question, realtimeMode);
        return true;
    }

    @Override public boolean startFromBackground(String question, ToolRouter.Host host) {
        host.logToolRoute("tv background intent hit text=" + question);
        host.openTv(question, true);
        return true;
    }
}
