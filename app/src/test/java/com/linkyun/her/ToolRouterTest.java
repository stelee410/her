package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class ToolRouterTest {
    @Test
    public void routesNewsBeforeWeatherWhenBothMatch() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("查一下新闻，也看看天气", true));

        assertEquals("news:查一下新闻，也看看天气:true", host.events.get(1));
    }

    @Test
    public void routesVolumeBeforeOtherTools() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("声音小一点，天气等会儿再说", true));

        assertEquals("volume:DOWN", host.events.get(1));
    }

    @Test
    public void routesVolumeUpAndDownCommands() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("声音大一点", true));
        assertTrue(router.routeUserText("音量调低一点", true));

        assertEquals("volume:UP", host.events.get(1));
        assertEquals("volume:DOWN", host.events.get(3));
    }

    @Test
    public void trimsUserTextBeforeRouting() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("  深圳天气怎么样  ", false));

        assertEquals("weather:深圳天气怎么样:false", host.events.get(1));
    }

    @Test
    public void blankUserTextDoesNotRouteToTools() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertFalse(router.routeUserText("   ", true));
        assertFalse(router.routeUserText(null, true));

        assertEquals("log:tool intent miss text=", host.events.get(0));
        assertEquals("log:tool intent miss text=", host.events.get(1));
    }

    @Test
    public void routesWeatherWhenNewsDoesNotMatch() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("深圳天气怎么样", false));

        assertEquals("weather:深圳天气怎么样:false", host.events.get(1));
    }

    @Test
    public void realtimeWeatherCanReuseLatestFact() {
        Host host = new Host();
        host.hasLatestWeatherFact = true;
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("那天气适合出门吗", true));

        assertEquals("reuseWeather:那天气适合出门吗", host.events.get(2));
    }

    @Test
    public void explicitWeatherRefreshDoesNotReuseLatestFact() {
        Host host = new Host();
        host.hasLatestWeatherFact = true;
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertTrue(router.routeUserText("重新查一下天气", true));

        assertEquals("weather:重新查一下天气:true", host.events.get(1));
    }

    @Test
    public void backgroundNewsRequiresKnownToolAndConfidence() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertFalse(router.routeBackgroundDecision("daily_news", 0.54, "新闻"));
        assertFalse(router.routeBackgroundDecision("weather", 0.99, "天气"));
        assertTrue(router.routeBackgroundDecision("daily_news", 0.55, "新闻"));

        assertEquals("backgroundNews:新闻", host.events.get(1));
    }

    @Test
    public void backgroundDecisionTrimsTextAndRejectsBlankQuestion() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertFalse(router.routeBackgroundDecision("daily_news", 0.99, "   "));
        assertTrue(router.routeBackgroundDecision("daily_news", 0.99, "  新闻  "));

        assertEquals("backgroundNews:新闻", host.events.get(1));
    }

    @Test
    public void noMatchReturnsFalse() {
        Host host = new Host();
        ToolRouter router = new ToolRouter(ToolRegistry.defaults(), host);

        assertFalse(router.routeUserText("今天聊点别的", false));

        assertEquals("log:tool intent miss text=今天聊点别的", host.events.get(0));
    }

    private static final class Host implements ToolRouter.Host {
        final List<String> events = new ArrayList<>();
        boolean hasLatestWeatherFact;

        @Override public boolean hasLatestWeatherFact() {
            return hasLatestWeatherFact;
        }

        @Override public void reuseLatestWeatherFact(String question) {
            events.add("reuseWeather:" + question);
        }

        @Override public void startNews(String question, boolean realtimeMode) {
            events.add("news:" + question + ":" + realtimeMode);
        }

        @Override public void startNewsFromBackground(String question) {
            events.add("backgroundNews:" + question);
        }

        @Override public void startWeather(String question, boolean realtimeMode) {
            events.add("weather:" + question + ":" + realtimeMode);
        }

        @Override public void adjustVoiceVolume(VolumeSkill.Direction direction) {
            events.add("volume:" + direction);
        }

        @Override public void logToolRoute(String message) {
            events.add("log:" + message);
        }
    }
}
