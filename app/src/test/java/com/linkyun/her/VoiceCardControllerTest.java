package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class VoiceCardControllerTest {
    @Test
    public void weatherCardReplacesNewsAndSchedulesTimeout() {
        Host host = new Host();
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);
        NewsTool.NewsResult news = new NewsTool.NewsResult("source", Collections.emptyList());
        WeatherTool.WeatherResult weather = weather("Shenzhen");

        controller.showNews(news, true);
        controller.showWeather(weather);

        assertSame(weather, controller.latestWeather());
        assertFalse(controller.hasNewsCard());
        assertTrue(scheduler.removed.size() > 0);
        assertTrue(host.refreshCount >= 2);
    }

    @Test
    public void weatherTimeoutOnlyClearsMatchingCard() {
        Host host = new Host();
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);
        WeatherTool.WeatherResult oldWeather = weather("Old");
        WeatherTool.WeatherResult newWeather = weather("New");

        controller.showWeather(oldWeather);
        Runnable oldTimeout = scheduler.scheduled.get(0);
        controller.showWeather(newWeather);

        oldTimeout.run();

        assertSame(newWeather, controller.latestWeather());
    }

    @Test
    public void newsCanStayVisibleWithoutTimeoutWhileAwaitingRealtimeAnswer() {
        Host host = new Host();
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);
        NewsTool.NewsResult news = new NewsTool.NewsResult("source", Collections.emptyList());

        controller.showNews(news, false);

        assertTrue(controller.hasNewsCard());
        assertTrue(scheduler.scheduled.isEmpty());
    }

    @Test
    public void newsTimeoutOnlyClearsMatchingCard() {
        Host host = new Host();
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);
        NewsTool.NewsResult oldNews = new NewsTool.NewsResult("old", Collections.emptyList());
        NewsTool.NewsResult newNews = new NewsTool.NewsResult("new", Collections.emptyList());

        controller.showNews(oldNews, true);
        Runnable oldTimeout = scheduler.scheduled.get(0);
        controller.showNews(newNews, true);

        oldTimeout.run();

        assertSame(newNews, controller.latestNews());
    }

    @Test
    public void cancelTimeoutsKeepsVisibleCards() {
        Host host = new Host();
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);
        WeatherTool.WeatherResult weather = weather("Shenzhen");

        controller.showWeather(weather);
        controller.cancelTimeouts();

        assertSame(weather, controller.latestWeather());
        assertTrue(controller.hasWeatherCard());
        assertEquals(1, scheduler.removed.size());
    }

    @Test
    public void timeoutClearDoesNotRefreshWhenVoiceSurfaceIsInactive() {
        Host host = new Host();
        host.active = false;
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);

        controller.showWeather(weather("Shenzhen"));
        scheduler.scheduled.get(0).run();

        assertFalse(controller.hasWeatherCard());
        assertEquals(0, host.refreshCount);
    }

    @Test
    public void clearAllCancelsTimeoutsAndRefreshesOnceWhenNeeded() {
        Host host = new Host();
        Scheduler scheduler = new Scheduler();
        VoiceCardController controller = new VoiceCardController(scheduler, host);

        controller.showWeather(weather("Shenzhen"));
        int beforeClear = host.refreshCount;
        controller.clearAll(true);

        assertFalse(controller.hasWeatherCard());
        assertFalse(controller.hasNewsCard());
        assertTrue(host.refreshCount > beforeClear);
        assertTrue(scheduler.removed.size() > 0);
    }

    private static WeatherTool.WeatherResult weather(String place) {
        return new WeatherTool.WeatherResult(place, 24, 25, 60, 8, "晴", "now");
    }

    private static final class Host implements VoiceCardController.Host {
        int refreshCount;
        boolean active = true;

        @Override public boolean isVoiceSurfaceActive() {
            return active;
        }

        @Override public void refreshVoiceHome() {
            refreshCount++;
        }
    }

    private static final class Scheduler implements VoiceCardController.Scheduler {
        final List<Runnable> scheduled = new ArrayList<>();
        final List<Runnable> removed = new ArrayList<>();

        @Override public void postDelayed(Runnable runnable, long delayMs) {
            scheduled.add(runnable);
        }

        @Override public void removeCallbacks(Runnable runnable) {
            removed.add(runnable);
        }
    }
}
