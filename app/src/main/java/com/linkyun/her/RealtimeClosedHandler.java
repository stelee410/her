package com.linkyun.her;

final class RealtimeClosedHandler {
    interface Host {
        void resetRealtimeOutput();
        boolean hasPendingToolTtsPlayback();
        boolean maybeStartToolTtsAfterRealtimeStopped();
        boolean hasActiveToolTtsPlayback();
        boolean isGatewayTtsPlaying();
        boolean isTextModeActive();
        boolean isInitializing();
        boolean isSummaryInProgress();
        void setState(String nextState);
    }

    private final Host host;

    RealtimeClosedHandler(Host host) {
        this.host = host;
    }

    void onClosed() {
        host.resetRealtimeOutput();
        if (host.isTextModeActive()) {
            host.setState("text_only");
            return;
        }
        if (host.hasPendingToolTtsPlayback()) {
            host.setState("ready");
            if (host.maybeStartToolTtsAfterRealtimeStopped()) return;
        }
        if (host.hasActiveToolTtsPlayback() || host.isGatewayTtsPlaying()) {
            host.setState("speaking");
            return;
        }
        if (host.isInitializing() && !host.isSummaryInProgress()) {
            host.setState("text_only");
            return;
        }
        host.setState(host.isSummaryInProgress() ? "summarizing" : "idle");
    }
}
