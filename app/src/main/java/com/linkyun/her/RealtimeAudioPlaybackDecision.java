package com.linkyun.her;

final class RealtimeAudioPlaybackDecision {
    private RealtimeAudioPlaybackDecision() {
    }

    static boolean shouldPlay(boolean textModeActive, boolean voiceSurfaceActive,
            boolean discardRealtimeAudio) {
        return !textModeActive && voiceSurfaceActive && !discardRealtimeAudio;
    }
}
