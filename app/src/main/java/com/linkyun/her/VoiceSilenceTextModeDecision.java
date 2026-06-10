package com.linkyun.her;

final class VoiceSilenceTextModeDecision {
    private VoiceSilenceTextModeDecision() {
    }

    static boolean shouldMonitor(boolean initialized,
            boolean voiceSurfaceActive,
            boolean textModeActive,
            boolean summaryInProgress,
            boolean activeToolTtsPlayback,
            boolean gatewayTtsPlaying,
            boolean weatherInteractionActive,
            boolean newsInteractionActive,
            VoiceSessionState state) {
        if (!initialized || !voiceSurfaceActive || textModeActive || summaryInProgress) return false;
        if (activeToolTtsPlayback || gatewayTtsPlaying ||
                weatherInteractionActive || newsInteractionActive) return false;
        return state != null && (state.isReady() || state.isListening());
    }
}
