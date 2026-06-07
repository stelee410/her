package com.linkyun.her;

final class TextModeEntryCleanupDecision {
    final boolean resetRealtimeRetries;
    final boolean stopToolTtsPlayback;
    final boolean stopRealtimeAudio;
    final boolean resetRealtimeOutput;
    final boolean clearVoiceInputRequests;
    final boolean clearPendingBroadcasts;

    private TextModeEntryCleanupDecision(boolean resetRealtimeRetries,
            boolean stopToolTtsPlayback,
            boolean stopRealtimeAudio,
            boolean resetRealtimeOutput,
            boolean clearVoiceInputRequests,
            boolean clearPendingBroadcasts) {
        this.resetRealtimeRetries = resetRealtimeRetries;
        this.stopToolTtsPlayback = stopToolTtsPlayback;
        this.stopRealtimeAudio = stopRealtimeAudio;
        this.resetRealtimeOutput = resetRealtimeOutput;
        this.clearVoiceInputRequests = clearVoiceInputRequests;
        this.clearPendingBroadcasts = clearPendingBroadcasts;
    }

    static TextModeEntryCleanupDecision enterTextMode() {
        return new TextModeEntryCleanupDecision(true, true, true, true, true, true);
    }
}
