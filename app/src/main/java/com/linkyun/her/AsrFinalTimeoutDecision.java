package com.linkyun.her;

final class AsrFinalTimeoutDecision {
    final boolean resumeListening;

    private AsrFinalTimeoutDecision(boolean resumeListening) {
        this.resumeListening = resumeListening;
    }

    static AsrFinalTimeoutDecision decide(boolean processing,
            boolean micRunning,
            boolean inputAudioOpen,
            boolean realtimeOutputActive,
            boolean summaryInProgress,
            boolean textModeActive) {
        if (!processing || micRunning || inputAudioOpen || realtimeOutputActive ||
                summaryInProgress || textModeActive) {
            return null;
        }
        return new AsrFinalTimeoutDecision(true);
    }
}
