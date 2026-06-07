package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeOutputCoordinatorTest {
    @Test
    public void startEntersStreamingAndSpeaking() {
        FakeHost host = new FakeHost();
        RealtimeOutputCoordinator coordinator = new RealtimeOutputCoordinator(host);

        coordinator.onStarted(24000, false);

        assertEquals(VoicePipelineState.RealtimeOutput.STREAMING, coordinator.state());
        assertTrue(coordinator.isActive());
        assertFalse(coordinator.shouldDiscardAudio());
        assertEquals(1, host.enterSpeakingCount);
        assertEquals(24000, host.lastSampleRate);
    }

    @Test
    public void startWhileToolTtsActiveInterruptsAndDiscardsUntilDone() {
        FakeHost host = new FakeHost();
        RealtimeOutputCoordinator coordinator = new RealtimeOutputCoordinator(host);

        coordinator.onStarted(24000, true);

        assertEquals(VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE, coordinator.state());
        assertTrue(coordinator.isActive());
        assertTrue(coordinator.shouldDiscardAudio());
        assertEquals(1, host.interruptCount);
        assertEquals("tts_already_playing", host.lastInterruptReason);
        assertTrue(host.lastInterruptDiscard);
        assertEquals(0, host.enterSpeakingCount);
    }

    @Test
    public void startOutsideVoiceSurfaceInterruptsAndDiscardsUntilDone() {
        FakeHost host = new FakeHost();
        host.voiceSurfaceActive = false;
        RealtimeOutputCoordinator coordinator = new RealtimeOutputCoordinator(host);

        coordinator.onStarted(24000, false);

        assertEquals(VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE, coordinator.state());
        assertTrue(coordinator.shouldDiscardAudio());
        assertEquals(1, host.interruptCount);
        assertEquals("voice_surface_inactive", host.lastInterruptReason);
        assertTrue(host.lastInterruptDiscard);
        assertEquals(0, host.enterSpeakingCount);
    }

    @Test
    public void doneClearsStreamingState() {
        FakeHost host = new FakeHost();
        RealtimeOutputCoordinator coordinator = new RealtimeOutputCoordinator(host);

        coordinator.onStarted(24000, false);
        coordinator.onDone();

        assertEquals(VoicePipelineState.RealtimeOutput.IDLE, coordinator.state());
        assertFalse(coordinator.isActive());
        assertFalse(coordinator.shouldDiscardAudio());
    }

    @Test
    public void stoppedClearsStateAndStopsPlayback() {
        FakeHost host = new FakeHost();
        RealtimeOutputCoordinator coordinator = new RealtimeOutputCoordinator(host);

        coordinator.onStarted(16000, false);
        coordinator.onStopped();

        assertEquals(VoicePipelineState.RealtimeOutput.IDLE, coordinator.state());
        assertEquals(1, host.stopCount);
    }

    @Test
    public void interruptedWithDiscardDropsAudioUntilDone() {
        FakeHost host = new FakeHost();
        RealtimeOutputCoordinator coordinator = new RealtimeOutputCoordinator(host);

        coordinator.markInterrupted(true);

        assertTrue(coordinator.shouldDiscardAudio());
        coordinator.onDone();
        assertFalse(coordinator.shouldDiscardAudio());
    }

    private static final class FakeHost implements RealtimeOutputCoordinator.Host {
        boolean voiceSurfaceActive = true;
        boolean externalTtsPlaying;
        int enterSpeakingCount;
        int lastSampleRate;
        int interruptCount;
        String lastInterruptReason;
        boolean lastInterruptDiscard;
        int stopCount;

        @Override public boolean isVoiceSurfaceActive() {
            return voiceSurfaceActive;
        }

        @Override public boolean isExternalTtsPlaying() {
            return externalTtsPlaying;
        }

        @Override public void interruptRealtimePlayback(String reason, boolean discardUntilDone) {
            interruptCount++;
            lastInterruptReason = reason;
            lastInterruptDiscard = discardUntilDone;
        }

        @Override public void enterRealtimeSpeaking(int sampleRate) {
            enterSpeakingCount++;
            lastSampleRate = sampleRate;
        }

        @Override public void stopRealtimeOutput() {
            stopCount++;
        }
    }
}
