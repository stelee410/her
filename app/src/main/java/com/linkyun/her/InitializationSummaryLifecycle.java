package com.linkyun.her;

final class InitializationSummaryLifecycle {
    private InitializationSummaryLifecycle() {
    }

    static boolean shouldStart(boolean summaryInProgress) {
        return !summaryInProgress;
    }

    static Flags active() {
        return new Flags(true, true);
    }

    static Flags cleared() {
        return new Flags(false, false);
    }

    static final class Flags {
        final boolean summaryInProgress;
        final boolean initSummaryPending;

        Flags(boolean summaryInProgress, boolean initSummaryPending) {
            this.summaryInProgress = summaryInProgress;
            this.initSummaryPending = initSummaryPending;
        }
    }
}
