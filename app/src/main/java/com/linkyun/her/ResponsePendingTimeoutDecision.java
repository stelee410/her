package com.linkyun.her;

final class ResponsePendingTimeoutDecision {
    final String nextState;
    final boolean scheduleContinuousListening;

    private ResponsePendingTimeoutDecision(String nextState, boolean scheduleContinuousListening) {
        this.nextState = nextState;
        this.scheduleContinuousListening = scheduleContinuousListening;
    }

    static ResponsePendingTimeoutDecision decide(boolean responsePending,
            boolean micRunning,
            boolean inputAudioOpen,
            boolean summaryInProgress,
            boolean textModeActive,
            boolean realtimeOpen) {
        if (!responsePending || micRunning || inputAudioOpen || summaryInProgress) {
            return null;
        }
        if (textModeActive) {
            return new ResponsePendingTimeoutDecision("text_only", false);
        }
        if (realtimeOpen) {
            return new ResponsePendingTimeoutDecision("ready", true);
        }
        return new ResponsePendingTimeoutDecision("idle", false);
    }
}
