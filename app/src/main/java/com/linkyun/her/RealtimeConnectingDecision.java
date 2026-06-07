package com.linkyun.her;

final class RealtimeConnectingDecision {
    private RealtimeConnectingDecision() {
    }

    static String nextState(boolean textModeActive) {
        return textModeActive ? "text_only" : "connecting";
    }
}
