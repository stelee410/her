package com.linkyun.her;

final class RealtimeOutputStartDecision {
    final boolean startRealtimeOutput;
    final String nextState;

    private RealtimeOutputStartDecision(boolean startRealtimeOutput, String nextState) {
        this.startRealtimeOutput = startRealtimeOutput;
        this.nextState = nextState;
    }

    static RealtimeOutputStartDecision decide(boolean textModeActive) {
        if (textModeActive) {
            return new RealtimeOutputStartDecision(false, "text_only");
        }
        return new RealtimeOutputStartDecision(true, null);
    }
}
