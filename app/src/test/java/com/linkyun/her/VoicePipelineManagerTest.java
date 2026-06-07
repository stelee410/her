package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class VoicePipelineManagerTest {
    @Test
    public void realtimeOutputStartsSpeakingWhenToolTtsIsIdle() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        VoicePipelineManager manager = new VoicePipelineManager(scheduler, host);

        manager.onRealtimeOutputStarted(24000);

        assertEquals(VoicePipelineState.RealtimeOutput.STREAMING, manager.realtimeOutputState());
        assertTrue(manager.isRealtimeOutputActive());
        assertEquals("enterRealtime:24000", host.events.get(0));
    }

    @Test
    public void pendingToolTtsCausesRealtimeStartToDiscardAndInterrupt() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.deferStart = true;
        host.speakingState = true;
        VoicePipelineManager manager = new VoicePipelineManager(scheduler, host);

        manager.queueToolTts("weather", "天气播报。");
        manager.onRealtimeOutputStarted(24000);

        assertEquals(VoicePipelineState.ToolTts.QUEUED, manager.toolTtsState());
        assertEquals(VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE,
                manager.realtimeOutputState());
        assertTrue(manager.shouldDiscardRealtimeAudio());
        assertEquals("interrupt:tts_already_playing:true", host.events.get(1));
        assertEquals(0, host.playCount);
    }

    @Test
    public void realtimeOutputOutsideVoiceSurfaceDiscardsWithoutSpeaking() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.voiceSurfaceActive = false;
        VoicePipelineManager manager = new VoicePipelineManager(scheduler, host);

        manager.onRealtimeOutputStarted(24000);

        assertEquals(VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE,
                manager.realtimeOutputState());
        assertTrue(manager.shouldDiscardRealtimeAudio());
        assertEquals("interrupt:voice_surface_inactive:true", host.events.get(0));
        assertEquals(0, host.playCount);
    }

    @Test
    public void realtimeStopBeforeToolTtsStartsPendingPlayback() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        host.realtimePlaybackActive = true;
        VoicePipelineManager manager = new VoicePipelineManager(scheduler, host);

        manager.queueToolTts("news", "新闻播报。");
        assertEquals(VoicePipelineState.ToolTts.WAITING_REALTIME_STOP, manager.toolTtsState());
        host.realtimePlaybackActive = false;

        assertTrue(manager.onRealtimeStoppedBeforeToolTts());

        assertEquals(VoicePipelineState.RealtimeOutput.IDLE, manager.realtimeOutputState());
        assertEquals(VoicePipelineState.ToolTts.REQUESTING, manager.toolTtsState());
        assertEquals(1, host.prepareCount);
        assertEquals(1, host.playCount);
    }

    @Test
    public void realtimeStopWithoutPendingToolTtsOnlyClearsRealtimeOutput() {
        ManualScheduler scheduler = new ManualScheduler();
        FakeHost host = new FakeHost();
        VoicePipelineManager manager = new VoicePipelineManager(scheduler, host);

        manager.onRealtimeOutputStarted(16000);
        assertFalse(manager.onRealtimeStoppedBeforeToolTts());

        assertEquals(VoicePipelineState.RealtimeOutput.IDLE, manager.realtimeOutputState());
        assertEquals(VoicePipelineState.ToolTts.IDLE, manager.toolTtsState());
        assertEquals(0, host.playCount);
    }

    private static final class ManualScheduler implements ToolTtsCoordinator.Scheduler {
        final List<Runnable> tasks = new ArrayList<>();

        @Override public void postDelayed(Runnable runnable, long delayMs) {
            tasks.add(runnable);
        }
    }

    private static final class FakeHost implements VoicePipelineManager.Host {
        final List<String> events = new ArrayList<>();
        boolean voiceSurfaceActive = true;
        boolean realtimePlaybackActive;
        boolean deferStart;
        boolean speakingState;
        boolean externalTtsPlaying;
        int prepareCount;
        int playCount;

        @Override public boolean isTextModeActive() {
            return false;
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
            events.add("log:" + message);
        }

        @Override public void interruptRealtimePlayback(String reason, boolean discardUntilDone) {
            events.add("interrupt:" + reason + ":" + discardUntilDone);
        }

        @Override public void prepareToolTtsPlayback() {
            prepareCount++;
            events.add("prepare");
        }

        @Override public void playToolTts(String id, String text,
                ToolTtsCoordinator.PlaybackListener listener) {
            playCount++;
            events.add("play:" + text);
        }

        @Override public void onToolTtsStarted(String id, String text) {
            events.add("toolStarted:" + text);
        }

        @Override public void onToolTtsFinished(String id) {
            events.add("toolFinished:" + id);
        }

        @Override public void resumeListeningAfterToolTts(long delayMs) {
            events.add("resume:" + delayMs);
        }

        @Override public boolean isExternalTtsPlaying() {
            return externalTtsPlaying;
        }

        @Override public void enterRealtimeSpeaking(int sampleRate) {
            events.add("enterRealtime:" + sampleRate);
        }

        @Override public void stopRealtimeOutput() {
            events.add("stopRealtime");
        }
    }
}
