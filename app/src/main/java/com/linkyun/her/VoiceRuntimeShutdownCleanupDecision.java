package com.linkyun.her;

final class VoiceRuntimeShutdownCleanupDecision {
    final boolean cancelVoiceInputRequests;
    final boolean clearToolInteractions;
    final boolean invalidateBackgroundToolRoute;
    final boolean cancelMemoryCompaction;
    final boolean cancelVoiceCardTimeouts;
    final boolean stopToolTtsPlayback;

    private VoiceRuntimeShutdownCleanupDecision(boolean cancelVoiceInputRequests,
            boolean clearToolInteractions,
            boolean invalidateBackgroundToolRoute,
            boolean cancelMemoryCompaction,
            boolean cancelVoiceCardTimeouts,
            boolean stopToolTtsPlayback) {
        this.cancelVoiceInputRequests = cancelVoiceInputRequests;
        this.clearToolInteractions = clearToolInteractions;
        this.invalidateBackgroundToolRoute = invalidateBackgroundToolRoute;
        this.cancelMemoryCompaction = cancelMemoryCompaction;
        this.cancelVoiceCardTimeouts = cancelVoiceCardTimeouts;
        this.stopToolTtsPlayback = stopToolTtsPlayback;
    }

    static VoiceRuntimeShutdownCleanupDecision shutdown() {
        return new VoiceRuntimeShutdownCleanupDecision(true, true, true, true, true, true);
    }
}
