package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class ToolTtsCoordinatorTest {
    @Test
    public void queueStartsPlaybackImmediatelyWhenRealtimeIsIdle() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "今天晴。");

        assertEquals(1, host.prepareCount);
        assertEquals(1, host.playCount);
        assertEquals(VoicePipelineState.ToolTts.REQUESTING, coordinator.state());
        assertFalse(coordinator.hasPendingPlayback());

        host.listener.onStarted(host.playedId, host.playedText);
        assertEquals(VoicePipelineState.ToolTts.PLAYING, coordinator.state());

        host.listener.onCompleted(host.playedId);
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(1, host.finishCount);
        assertEquals(350L, host.lastResumeDelay);
    }

    @Test
    public void queueWaitsForRealtimeOutputThenStartsAfterStop() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("news", "新闻来了。");

        assertTrue(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.WAITING_REALTIME_STOP, coordinator.state());
        assertEquals(1, host.interruptCount);
        assertEquals(0, host.playCount);

        host.realtimePlaybackActive = false;
        assertTrue(coordinator.onRealtimeStopped());

        assertEquals(1, host.prepareCount);
        assertEquals(1, host.playCount);
        assertEquals(VoicePipelineState.ToolTts.REQUESTING, coordinator.state());
    }

    @Test
    public void queueIsIgnoredInTextMode() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.textMode = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "不该播。");

        assertFalse(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(0, host.playCount);
    }

    @Test
    public void queueIsIgnoredOutsideVoiceSurface() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.voiceSurfaceActive = false;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "不该播。");

        assertFalse(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(0, host.playCount);
    }

    @Test
    public void queueForceStartsIfRealtimeStopCallbackIsMissing() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "要强制播放。");
        scheduler.runDelay(220);

        assertEquals(1, host.interruptCount);
        assertEquals(1, host.prepareCount);
        assertEquals(1, host.playCount);
        assertEquals(VoicePipelineState.ToolTts.REQUESTING, coordinator.state());
    }

    @Test
    public void forceStartClearsPendingWhenTextModeBecameActive() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "旧播报。");
        host.textMode = true;
        scheduler.runDelay(220);

        assertFalse(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(0, host.playCount);
        assertEquals(1, host.finishCount);
    }

    @Test
    public void forceStartClearsPendingWhenVoiceSurfaceBecameInactive() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "旧播报。");
        host.voiceSurfaceActive = false;
        scheduler.runDelay(220);

        assertFalse(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(0, host.playCount);
        assertEquals(1, host.finishCount);
    }

    @Test
    public void staleForceStartDoesNotStartReplacementPendingPlayback() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "旧播报。");
        Runnable staleForceStart = scheduler.tasks.get(0).runnable;
        coordinator.stop(false);
        coordinator.queue("news", "新播报。");

        staleForceStart.run();

        assertEquals(0, host.playCount);
        assertTrue(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.WAITING_REALTIME_STOP, coordinator.state());
    }

    @Test
    public void queueDefersWhileSpeakingAndStartsWhenSpeechHasStopped() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.deferStart = true;
        host.speakingState = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("news", "延迟播放。");
        assertEquals(0, host.playCount);

        host.speakingState = false;
        scheduler.runDelay(3200);

        assertEquals(1, host.prepareCount);
        assertEquals(1, host.playCount);
    }

    @Test
    public void deferredStartClearsPendingWhenTextModeBecameActive() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.deferStart = true;
        host.speakingState = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("news", "延迟播放。");
        host.speakingState = false;
        host.textMode = true;
        scheduler.runDelay(3200);

        assertFalse(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(0, host.playCount);
        assertEquals(1, host.finishCount);
    }

    @Test
    public void deferredStartClearsPendingWhenVoiceSurfaceBecameInactive() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.deferStart = true;
        host.speakingState = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("news", "延迟播放。");
        host.speakingState = false;
        host.voiceSurfaceActive = false;
        scheduler.runDelay(3200);

        assertFalse(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(0, host.playCount);
        assertEquals(1, host.finishCount);
    }

    @Test
    public void staleDeferredStartDoesNotStartReplacementPendingPlayback() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.deferStart = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "旧延迟。");
        Runnable staleDeferredStart = scheduler.tasks.get(0).runnable;
        coordinator.stop(false);
        coordinator.queue("news", "新延迟。");

        staleDeferredStart.run();

        assertEquals(0, host.playCount);
        assertTrue(coordinator.hasPendingPlayback());
        assertEquals(VoicePipelineState.ToolTts.QUEUED, coordinator.state());
    }

    @Test
    public void stopClearsPendingAndCanResumeListening() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("news", "取消这段。");
        coordinator.stop(true);

        assertFalse(coordinator.hasPendingPlayback());
        assertFalse(coordinator.isPlaybackActive());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(80L, host.lastResumeDelay);
    }

    @Test
    public void stopOutsideVoiceSurfaceDoesNotResumeListening() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("news", "取消这段。");
        host.voiceSurfaceActive = false;
        coordinator.stop(true);

        assertFalse(coordinator.hasPendingPlayback());
        assertFalse(coordinator.isPlaybackActive());
        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(-1, host.lastResumeDelay);
    }

    @Test
    public void staleCompletionAfterStopDoesNotResumeListeningAgain() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "先播报。");
        ToolTtsCoordinator.PlaybackListener oldListener = host.listener;
        String oldId = host.playedId;
        oldListener.onStarted(oldId, host.playedText);
        coordinator.stop(false);

        oldListener.onCompleted(oldId);

        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(1, host.finishCount);
        assertEquals(-1, host.lastResumeDelay);
    }

    @Test
    public void staleCompletionFromPreviousPlaybackDoesNotFinishNewPlayback() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "第一段。");
        ToolTtsCoordinator.PlaybackListener oldListener = host.listener;
        String oldId = host.playedId;
        coordinator.queue("news", "第二段。");
        ToolTtsCoordinator.PlaybackListener newListener = host.listener;
        String newId = host.playedId;

        oldListener.onCompleted(oldId);

        assertEquals(VoicePipelineState.ToolTts.REQUESTING, coordinator.state());
        assertEquals(0, host.finishCount);

        newListener.onCompleted(newId);

        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(1, host.finishCount);
        assertEquals(350L, host.lastResumeDelay);
    }

    @Test
    public void completionInTextModeDoesNotResumeListening() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "先播报。");
        host.listener.onStarted(host.playedId, host.playedText);
        host.textMode = true;
        host.listener.onCompleted(host.playedId);

        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(1, host.finishCount);
        assertEquals(-1, host.lastResumeDelay);
    }

    @Test
    public void completionOutsideVoiceSurfaceDoesNotResumeListening() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "先播报。");
        host.listener.onStarted(host.playedId, host.playedText);
        host.voiceSurfaceActive = false;
        host.listener.onCompleted(host.playedId);

        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(1, host.finishCount);
        assertEquals(-1, host.lastResumeDelay);
    }

    @Test
    public void startedCallbackOutsideVoiceSurfaceFinishesWithoutPlayingState() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        ToolTtsCoordinator coordinator = new ToolTtsCoordinator(scheduler, host);

        coordinator.queue("weather", "先播报。");
        host.voiceSurfaceActive = false;
        host.listener.onStarted(host.playedId, host.playedText);

        assertEquals(VoicePipelineState.ToolTts.IDLE, coordinator.state());
        assertEquals(1, host.finishCount);
        assertEquals(-1, host.lastResumeDelay);
    }

    private static final class ManualScheduler implements ToolTtsCoordinator.Scheduler {
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

    private static final class FakeHost implements ToolTtsCoordinator.Host {
        boolean textMode;
        boolean voiceSurfaceActive = true;
        boolean realtimePlaybackActive;
        boolean deferStart;
        boolean speakingState;
        int interruptCount;
        int prepareCount;
        int playCount;
        int finishCount;
        long lastResumeDelay = -1;
        String playedId;
        String playedText;
        ToolTtsCoordinator.PlaybackListener listener;

        @Override public boolean isTextModeActive() {
            return textMode;
        }

        @Override public boolean isVoiceSurfaceActive() {
            return voiceSurfaceActive;
        }

        @Override public boolean isRealtimePlaybackActive() {
            return realtimePlaybackActive;
        }

        @Override public boolean shouldDeferStart() {
            return deferStart;
        }

        @Override public boolean isSpeakingState() {
            return speakingState;
        }

        @Override public void logToolTts(String message) {
        }

        @Override public void interruptRealtimePlayback(String reason, boolean discardUntilDone) {
            interruptCount++;
        }

        @Override public void prepareToolTtsPlayback() {
            prepareCount++;
        }

        @Override public void playToolTts(String id, String text, ToolTtsCoordinator.PlaybackListener listener) {
            playCount++;
            playedId = id;
            playedText = text;
            this.listener = listener;
        }

        @Override public void onToolTtsStarted(String id, String text) {
        }

        @Override public void onToolTtsFinished(String id) {
            finishCount++;
        }

        @Override public void resumeListeningAfterToolTts(long delayMs) {
            lastResumeDelay = delayMs;
        }
    }
}
