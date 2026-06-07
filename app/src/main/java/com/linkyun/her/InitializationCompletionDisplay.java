package com.linkyun.her;

final class InitializationCompletionDisplay {
    static final class State {
        final String progressText;
        final String lastTurnText;
        final int audioLevel;
        final long homeDelayMs;

        State(String progressText, String lastTurnText, int audioLevel, long homeDelayMs) {
            this.progressText = progressText;
            this.lastTurnText = lastTurnText;
            this.audioLevel = audioLevel;
            this.homeDelayMs = homeDelayMs;
        }
    }

    private InitializationCompletionDisplay() {
    }

    static State completed() {
        return new State(
                "初始化完成",
                "我记住啦。我们从这里重新开始。",
                100,
                5000);
    }
}
