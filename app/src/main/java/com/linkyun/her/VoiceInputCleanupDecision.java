package com.linkyun.her;

final class VoiceInputCleanupDecision {
    final boolean cancelAsrFinalTimeout;
    final boolean clearPendingStart;
    final boolean cancelContinuousListening;

    private VoiceInputCleanupDecision(boolean cancelAsrFinalTimeout,
            boolean clearPendingStart,
            boolean cancelContinuousListening) {
        this.cancelAsrFinalTimeout = cancelAsrFinalTimeout;
        this.clearPendingStart = clearPendingStart;
        this.cancelContinuousListening = cancelContinuousListening;
    }

    static VoiceInputCleanupDecision clearRequests(boolean hasVoiceInput) {
        return new VoiceInputCleanupDecision(true, hasVoiceInput, hasVoiceInput);
    }
}
