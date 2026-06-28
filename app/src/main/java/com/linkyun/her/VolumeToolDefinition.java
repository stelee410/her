package com.linkyun.her;

final class VolumeToolDefinition implements ToolDefinition {
    static final String ID = "volume";
    static final String LLM_TOOL_ID_UP = "volume_up";
    static final String LLM_TOOL_ID_DOWN = "volume_down";

    @Override public String id() {
        return ID;
    }

    @Override public boolean matches(String text) {
        return VolumeSkill.isVolumeCommand(text);
    }

    @Override public boolean matchesBackgroundTool(String toolId) {
        return LLM_TOOL_ID_UP.equals(toolId) || LLM_TOOL_ID_DOWN.equals(toolId);
    }

    @Override public boolean start(String question, boolean realtimeMode, ToolRouter.Host host) {
        VolumeSkill.Direction direction = VolumeSkill.direction(question);
        if (direction == null) return false;
        host.logToolRoute("volume intent hit direction=" + direction
                + " realtime=" + realtimeMode + " text=" + question);
        host.adjustVoiceVolume(direction);
        return true;
    }

    @Override public boolean startFromBackground(String question, ToolRouter.Host host) {
        return start(question, true, host);
    }
}
