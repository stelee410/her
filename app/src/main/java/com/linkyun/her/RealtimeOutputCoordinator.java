package com.linkyun.her;

final class RealtimeOutputCoordinator {
    interface Host {
        boolean isVoiceSurfaceActive();
        boolean isExternalTtsPlaying();
        void interruptRealtimePlayback(String reason, boolean discardUntilDone);
        void enterRealtimeSpeaking(int sampleRate);
        void stopRealtimeOutput();
    }

    private final Host host;
    private VoicePipelineState.RealtimeOutput state = VoicePipelineState.RealtimeOutput.IDLE;

    RealtimeOutputCoordinator(Host host) {
        this.host = host;
    }

    VoicePipelineState.RealtimeOutput state() {
        return state;
    }

    boolean isActive() {
        return state != VoicePipelineState.RealtimeOutput.IDLE;
    }

    boolean shouldDiscardAudio() {
        return state == VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE;
    }

    void markInterrupted(boolean discardUntilDone) {
        if (discardUntilDone) {
            state = VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE;
        }
    }

    void reset() {
        state = VoicePipelineState.RealtimeOutput.IDLE;
    }

    void onStarted(int sampleRate, boolean toolTtsActive) {
        if (!host.isVoiceSurfaceActive()) {
            state = VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE;
            host.interruptRealtimePlayback("voice_surface_inactive", true);
            return;
        }
        if (toolTtsActive || host.isExternalTtsPlaying()) {
            state = VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE;
            host.interruptRealtimePlayback("tts_already_playing", true);
            return;
        }
        if (state == VoicePipelineState.RealtimeOutput.DISCARDING_UNTIL_DONE) return;
        state = VoicePipelineState.RealtimeOutput.STREAMING;
        host.enterRealtimeSpeaking(sampleRate);
    }

    void onDone() {
        state = VoicePipelineState.RealtimeOutput.IDLE;
    }

    void onStopped() {
        state = VoicePipelineState.RealtimeOutput.IDLE;
        host.stopRealtimeOutput();
    }
}
