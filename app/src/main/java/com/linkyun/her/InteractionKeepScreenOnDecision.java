package com.linkyun.her;

final class InteractionKeepScreenOnDecision {
    private InteractionKeepScreenOnDecision() { }

    static boolean shouldKeepScreenOn(boolean initializing,
            boolean summaryInProgress,
            boolean micRunning,
            boolean inputAudioOpen,
            boolean activeToolTtsPlayback,
            boolean weatherInteractionActive,
            boolean newsInteractionActive,
            VoiceSessionState voiceState) {
        if (initializing || summaryInProgress) return true;
        if (micRunning || inputAudioOpen) return true;
        if (activeToolTtsPlayback) return true;
        if (weatherInteractionActive || newsInteractionActive) return true;
        return voiceState != null && voiceState.shouldKeepScreenOn();
    }
}
