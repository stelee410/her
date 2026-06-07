package com.linkyun.her;

interface ToolDefinition {
    String id();
    boolean matches(String text);
    boolean matchesBackgroundTool(String toolId);
    boolean start(String question, boolean realtimeMode, ToolRouter.Host host);
    boolean startFromBackground(String question, ToolRouter.Host host);
}
