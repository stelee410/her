package com.linkyun.her;

final class VoiceAutoStartDecision {
    private VoiceAutoStartDecision() {
    }

    enum OpeningAction {
        SKIP,
        START_LISTENING,
        PROMPT_HEADSET
    }

    static boolean shouldStartOnVoiceSurface(
            boolean initialized,
            boolean initializing,
            boolean summaryInProgress,
            boolean hasActiveToolTtsPlayback,
            boolean isGatewayTtsPlaying,
            boolean micRunning,
            boolean inputAudioOpen,
            boolean boundHeadsetConnected) {
        return initialized
                && !initializing
                && !summaryInProgress
                && !hasActiveToolTtsPlayback
                && !isGatewayTtsPlaying
                && !micRunning
                && !inputAudioOpen
                && boundHeadsetConnected;
    }

    static OpeningAction afterInitializationOpening(
            boolean initializing,
            boolean summaryInProgress,
            boolean boundHeadsetConnected) {
        if (!initializing || summaryInProgress) return OpeningAction.SKIP;
        if (!boundHeadsetConnected) return OpeningAction.PROMPT_HEADSET;
        return OpeningAction.START_LISTENING;
    }
}
