package com.linkyun.her;

final class RealtimeProviderStateDecision {
    final boolean logThinking;
    final boolean outputStarted;
    final String nextState;

    private RealtimeProviderStateDecision(boolean logThinking, boolean outputStarted, String nextState) {
        this.logThinking = logThinking;
        this.outputStarted = outputStarted;
        this.nextState = nextState;
    }

    static RealtimeProviderStateDecision decide(String providerState,
            boolean hasActiveToolTtsPlayback,
            boolean isGatewayTtsPlaying,
            boolean isMicRunning,
            boolean textModeActive,
            boolean voiceSurfaceActive) {
        String state = providerState == null ? "ready" : providerState;
        if ("thinking".equals(state)) {
            return new RealtimeProviderStateDecision(true, false, null);
        }
        if (hasActiveToolTtsPlayback || isGatewayTtsPlaying) {
            return new RealtimeProviderStateDecision(false, false, null);
        }
        if (textModeActive) {
            return new RealtimeProviderStateDecision(false, false, "text_only");
        }
        if (!voiceSurfaceActive) {
            return new RealtimeProviderStateDecision(false, false, null);
        }
        if ("tts_streaming".equals(state)) {
            return new RealtimeProviderStateDecision(false, true, null);
        }
        if ("idle".equals(state) || "listening".equals(state)) {
            return new RealtimeProviderStateDecision(false, false, isMicRunning ? "listening" : "ready");
        }
        return new RealtimeProviderStateDecision(false, false, null);
    }
}
