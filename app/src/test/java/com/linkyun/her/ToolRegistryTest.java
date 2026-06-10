package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class ToolRegistryTest {
    @Test
    public void defaultsRegisterNewsWeatherAndBackgroundNews() {
        ToolRegistry registry = ToolRegistry.defaults();

        assertEquals(VolumeToolDefinition.ID, registry.match("声音大一点").id());
        assertEquals(NewsToolDefinition.ID, registry.match("查一下新闻").id());
        assertEquals(WeatherToolDefinition.ID, registry.match("深圳天气怎么样").id());
        assertEquals(NewsToolDefinition.ID, registry.backgroundTool("daily_news").id());
        assertNull(registry.backgroundTool("weather"));
    }

    @Test
    public void registryDefensivelyCopiesDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();
        definitions.add(new FakeTool("fake", "ping", "background_ping"));
        ToolRegistry registry = new ToolRegistry(definitions);

        definitions.clear();

        assertEquals("fake", registry.match("ping").id());
        assertEquals("fake", registry.backgroundTool("background_ping").id());
    }

    @Test
    public void unknownTextAndBackgroundToolMiss() {
        ToolRegistry registry = ToolRegistry.defaults();

        assertNull(registry.match("今天聊点别的"));
        assertNull(registry.backgroundTool("daily_weather"));
    }

    private static final class FakeTool implements ToolDefinition {
        private final String id;
        private final String matchText;
        private final String backgroundToolId;

        FakeTool(String id, String matchText, String backgroundToolId) {
            this.id = id;
            this.matchText = matchText;
            this.backgroundToolId = backgroundToolId;
        }

        @Override public String id() {
            return id;
        }

        @Override public boolean matches(String text) {
            return matchText.equals(text);
        }

        @Override public boolean matchesBackgroundTool(String toolId) {
            return backgroundToolId.equals(toolId);
        }

        @Override public boolean start(String question, boolean realtimeMode, ToolRouter.Host host) {
            return true;
        }

        @Override public boolean startFromBackground(String question, ToolRouter.Host host) {
            return true;
        }
    }
}
