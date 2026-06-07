package com.linkyun.her;

final class VoiceSurfaceExitCleanupDecision {
    final boolean cancelVoiceInputRequests;
    final boolean stopActiveInput;
    final String nextInputState;
    final boolean stopToolTtsPlayback;
    final boolean stopRealtimeAudio;
    final boolean resetRealtimeOutput;
    final boolean clearVoiceSurfaceViews;

    private VoiceSurfaceExitCleanupDecision(boolean cancelVoiceInputRequests,
            boolean stopActiveInput,
            String nextInputState,
            boolean stopToolTtsPlayback,
            boolean stopRealtimeAudio,
            boolean resetRealtimeOutput,
            boolean clearVoiceSurfaceViews) {
        this.cancelVoiceInputRequests = cancelVoiceInputRequests;
        this.stopActiveInput = stopActiveInput;
        this.nextInputState = nextInputState;
        this.stopToolTtsPlayback = stopToolTtsPlayback;
        this.stopRealtimeAudio = stopRealtimeAudio;
        this.resetRealtimeOutput = resetRealtimeOutput;
        this.clearVoiceSurfaceViews = clearVoiceSurfaceViews;
    }

    static VoiceSurfaceExitCleanupDecision leaveVoiceSurface(boolean inputActive) {
        return new VoiceSurfaceExitCleanupDecision(
                true, inputActive, "ready", true, true, true, true);
    }
}
