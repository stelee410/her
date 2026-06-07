package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class PendingBroadcastCoordinatorTest {
    @Test
    public void weatherOnlySendsLatestScheduledPrompt() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("old");
        coordinator.queueWeather("new");
        scheduler.runDelay(1000);

        assertEquals(1, host.sentTexts.size());
        assertEquals("new", host.sentTexts.get(0));
    }

    @Test
    public void clearedWeatherPromptDoesNotSendCapturedPrompt() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("weather");
        coordinator.clearWeather();
        scheduler.runDelay(1000);

        assertEquals(0, host.sentTexts.size());
    }

    @Test
    public void textModeDropsPendingWeatherWithoutConnectingOrSending() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.textMode = true;
        host.realtimeOpen = false;
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("weather");
        scheduler.runDelay(1000);

        assertEquals(0, host.connectCount);
        assertEquals(0, host.sentTexts.size());
        assertEquals(false, coordinator.hasPendingWeather());
    }

    @Test
    public void weatherRetriesWhileBusyThenSends() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("weather");
        host.weatherReady = false;
        scheduler.runDelay(1000);
        assertEquals(0, host.sentTexts.size());

        host.weatherReady = true;
        scheduler.runDelay(600);

        assertEquals(1, host.sentTexts.size());
        assertEquals("weather", host.sentTexts.get(0));
    }

    @Test
    public void clearedWeatherPromptDoesNotSendAfterBusyRetry() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("weather");
        host.weatherReady = false;
        scheduler.runDelay(1000);
        coordinator.clearWeather();

        host.weatherReady = true;
        scheduler.runDelay(600);

        assertEquals(0, host.sentTexts.size());
    }

    @Test
    public void newsRetriesWhileBusyThenSends() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueNews("news");
        host.newsReady = false;
        scheduler.runDelay(1000);
        assertEquals(0, host.sentTexts.size());

        host.newsReady = true;
        scheduler.runDelay(600);
        assertEquals(1, host.sentTexts.size());
        assertEquals("news", host.sentTexts.get(0));
    }

    @Test
    public void clearedNewsPromptDoesNotSendAfterBusyRetry() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueNews("news");
        host.newsReady = false;
        scheduler.runDelay(1000);
        coordinator.clearNews();

        host.newsReady = true;
        scheduler.runDelay(600);

        assertEquals(0, host.sentTexts.size());
    }

    @Test
    public void textModeDropsPendingNewsWithoutConnectingOrSending() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.textMode = true;
        host.realtimeOpen = false;
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueNews("news");
        scheduler.runDelay(1000);

        assertEquals(0, host.connectCount);
        assertEquals(0, host.sentTexts.size());
        assertEquals(false, coordinator.hasPendingNews());
    }

    @Test
    public void newsConnectsRealtimeBeforeSendingAndPushesFactAgain() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimeOpen = false;
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueNews("news");
        scheduler.runDelay(1000);
        assertEquals(1, host.connectCount);
        assertEquals(0, host.sentTexts.size());

        host.realtimeOpen = true;
        scheduler.runDelay(1400);

        assertEquals(1, host.sentTexts.size());
        assertEquals("news", host.sentTexts.get(0));
        assertEquals(2, host.newsFactPushCount);
    }

    @Test
    public void weatherConnectsRealtimeBeforeSending() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimeOpen = false;
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("weather");
        scheduler.runDelay(1000);
        assertEquals(1, host.connectCount);
        assertEquals(0, host.sentTexts.size());

        host.realtimeOpen = true;
        scheduler.runDelay(1400);
        assertEquals(1, host.sentTexts.size());
        assertEquals("weather", host.sentTexts.get(0));
        assertEquals(2, host.weatherFactPushCount);
    }

    @Test
    public void weatherPushesFactAgainBeforeSendingWhenRealtimeWasAlreadyOpen() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        PendingBroadcastCoordinator coordinator = new PendingBroadcastCoordinator(scheduler, host);

        coordinator.queueWeather("weather");
        scheduler.runDelay(1000);

        assertEquals(1, host.sentTexts.size());
        assertEquals(2, host.weatherFactPushCount);
    }

    private static final class ManualScheduler implements PendingBroadcastCoordinator.Scheduler {
        private final List<Task> tasks = new ArrayList<>();

        @Override public void postDelayed(Runnable runnable, long delayMs) {
            tasks.add(new Task(runnable, delayMs));
        }

        void runDelay(long delayMs) {
            List<Task> ready = new ArrayList<>();
            for (Task task : tasks) {
                if (task.delayMs == delayMs) ready.add(task);
            }
            tasks.removeAll(ready);
            for (Task task : ready) {
                task.runnable.run();
            }
        }
    }

    private static final class Task {
        final Runnable runnable;
        final long delayMs;

        Task(Runnable runnable, long delayMs) {
            this.runnable = runnable;
            this.delayMs = delayMs;
        }
    }

    private static final class FakeHost implements PendingBroadcastCoordinator.Host {
        boolean textMode;
        boolean weatherReady = true;
        boolean newsReady = true;
        boolean realtimeOpen = true;
        int connectCount;
        int weatherFactPushCount;
        int newsFactPushCount;
        final List<String> sentTexts = new ArrayList<>();

        @Override public boolean isTextModeActive() {
            return textMode;
        }

        @Override public boolean canSendWeatherNow() {
            return weatherReady;
        }

        @Override public boolean canSendNewsNow() {
            return newsReady;
        }

        @Override public boolean isRealtimeOpen() {
            return realtimeOpen;
        }

        @Override public void connectRealtime() {
            connectCount++;
        }

        @Override public void pushWeatherFact() {
            weatherFactPushCount++;
        }

        @Override public void pushNewsFact() {
            newsFactPushCount++;
        }

        @Override public void sendRealtimeText(String text) {
            sentTexts.add(text);
        }

        @Override public void onBroadcastSent() {
        }

        @Override public void logBroadcast(String message) {
        }
    }
}
