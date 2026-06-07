package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class VoiceInputCoordinatorTest {
    @Test
    public void textModeStopsInputAndClearsPendingStart() {
        Host host = new Host();
        host.inputActive = true;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(true, "manual", true);
        coordinator.enterTextMode();

        assertFalse(coordinator.hasPendingStart());
        assertEquals("stop:text_only", host.events.get(0));
    }

    @Test
    public void textModeBlocksDelayedRealtimeReadyStart() {
        Host host = new Host();
        host.realtimeOpen = false;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(true, "manual", true);
        assertTrue(coordinator.hasPendingStart());

        host.textMode = true;
        host.realtimeOpen = true;
        assertTrue(coordinator.onRealtimeReady());

        assertFalse(coordinator.hasPendingStart());
        assertEquals(0, host.startCount);
    }

    @Test
    public void startsPendingRequestWhenRealtimeBecomesReady() {
        Host host = new Host();
        host.realtimeOpen = false;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(true, "user_speech_detected", true);
        assertEquals("connect", host.events.get(0));

        host.realtimeOpen = true;
        assertTrue(coordinator.onRealtimeReady());

        assertEquals(1, host.startCount);
        assertEquals("prepare:user_speech_detected", host.events.get(1));
        assertEquals("start", host.events.get(2));
    }

    @Test
    public void scheduledContinuousListeningDoesNotStartInTextMode() {
        Host host = new Host();
        host.textMode = true;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.scheduleContinuousListening(100);
        host.runScheduled();

        assertEquals(0, host.startCount);
    }

    @Test
    public void scheduledContinuousListeningStartsWhenReady() {
        Host host = new Host();
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.scheduleContinuousListening(100);
        host.runScheduled();

        assertEquals(1, host.startCount);
    }

    @Test
    public void automaticStartWithoutPermissionDoesNotRemainPending() {
        Host host = new Host();
        host.permission = false;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(false, null, false);

        assertFalse(coordinator.hasPendingStart());
        assertEquals(0, host.events.size());
    }

    @Test
    public void deniedPermissionClearsPendingStart() {
        Host host = new Host();
        host.permission = false;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(true, "manual", true);
        assertTrue(coordinator.hasPendingStart());

        coordinator.onRecordPermissionDenied();

        assertFalse(coordinator.hasPendingStart());
    }

    @Test
    public void permissionGrantAfterTextModeDoesNotStartInput() {
        Host host = new Host();
        host.permission = false;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(true, "manual", true);
        assertTrue(coordinator.hasPendingStart());

        host.textMode = true;
        host.permission = true;
        coordinator.onRecordPermissionGranted();

        assertFalse(coordinator.hasPendingStart());
        assertEquals(0, host.startCount);
    }

    @Test
    public void headsetPromptIsOnlyShownForPromptedStarts() {
        Host host = new Host();
        host.bound = false;
        VoiceInputCoordinator coordinator = new VoiceInputCoordinator(host, host, true);

        coordinator.requestStart(false, null, false);
        assertEquals(0, host.events.size());

        coordinator.requestStart(true, "manual", true);
        assertEquals("headset", host.events.get(0));
        assertFalse(coordinator.hasPendingStart());
    }

    private static final class Host implements VoiceInputCoordinator.Scheduler, VoiceInputCoordinator.Host {
        final List<String> events = new ArrayList<>();
        final List<Runnable> scheduled = new ArrayList<>();
        boolean textMode;
        boolean inputActive;
        boolean realtimeOpen = true;
        boolean bound = true;
        boolean permission = true;
        boolean toolTts;
        boolean ready = true;
        boolean voiceSurface = true;
        int startCount;

        @Override public void postDelayed(Runnable runnable, long delayMs) {
            scheduled.add(runnable);
        }

        void runScheduled() {
            List<Runnable> tasks = new ArrayList<>(scheduled);
            scheduled.clear();
            for (Runnable task : tasks) task.run();
        }

        @Override public boolean isTextModeActive() { return textMode; }
        @Override public boolean isInputActive() { return inputActive; }
        @Override public boolean isRealtimeOpen() { return realtimeOpen; }
        @Override public boolean isBoundHeadsetConnected() { return bound; }
        @Override public boolean hasRecordPermission() { return permission; }
        @Override public boolean hasActiveToolTtsPlayback() { return toolTts; }
        @Override public boolean isReadyForContinuousListening() { return ready; }
        @Override public boolean isVoiceSurfaceActive() { return voiceSurface; }
        @Override public void requestRecordPermission() { events.add("permission"); }
        @Override public void connectRealtime() { events.add("connect"); }
        @Override public void prepareInputStart(String interruptReason) { events.add("prepare:" + interruptReason); }
        @Override public void startInputAudio() { startCount++; events.add("start"); }
        @Override public void stopInputAudio(String nextState) { inputActive = false; events.add("stop:" + nextState); }
        @Override public void setState(String nextState) { events.add("state:" + nextState); }
        @Override public void showHeadsetPrompt() { events.add("headset"); }
        @Override public void logVoiceInput(String message) { }
    }
}
