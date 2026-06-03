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
        boolean weatherReady = true;
        boolean newsReady = true;
        boolean realtimeOpen = true;
        int connectCount;
        final List<String> sentTexts = new ArrayList<>();

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
        }

        @Override public void pushNewsFact() {
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
