package com.linkyun.her;

final class RealtimeAssistantDeltaDecision {
    final boolean appendDelta;
    final boolean discardActiveDraft;
    final boolean refreshAfterDiscard;

    private RealtimeAssistantDeltaDecision(boolean appendDelta,
            boolean discardActiveDraft,
            boolean refreshAfterDiscard) {
        this.appendDelta = appendDelta;
        this.discardActiveDraft = discardActiveDraft;
        this.refreshAfterDiscard = refreshAfterDiscard;
    }

    static RealtimeAssistantDeltaDecision decide(boolean textModeActive, boolean hasActiveDraft) {
        if (textModeActive) {
            return new RealtimeAssistantDeltaDecision(false, hasActiveDraft, hasActiveDraft);
        }
        return new RealtimeAssistantDeltaDecision(true, false, false);
    }
}
