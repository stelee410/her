package com.linkyun.her;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ToolRegistry {
    private final List<ToolDefinition> tools;

    ToolRegistry(List<ToolDefinition> tools) {
        this.tools = Collections.unmodifiableList(new ArrayList<>(tools));
    }

    static ToolRegistry defaults() {
        List<ToolDefinition> definitions = new ArrayList<>();
        definitions.add(new VolumeToolDefinition());
        definitions.add(new NewsToolDefinition());
        definitions.add(new WeatherToolDefinition());
        return new ToolRegistry(definitions);
    }

    ToolDefinition match(String text) {
        for (ToolDefinition tool : tools) {
            if (tool.matches(text)) return tool;
        }
        return null;
    }

    ToolDefinition backgroundTool(String toolId) {
        for (ToolDefinition tool : tools) {
            if (tool.matchesBackgroundTool(toolId)) return tool;
        }
        return null;
    }
}
