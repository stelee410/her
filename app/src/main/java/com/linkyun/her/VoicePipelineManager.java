package com.linkyun.her;

final class VoicePipelineManager {
    interface Host extends ToolTtsCoordinator.Host {
    }

    private final ToolTtsCoordinator toolTts;

    VoicePipelineManager(ToolTtsCoordinator.Scheduler scheduler, Host host) {
        toolTts = new ToolTtsCoordinator(scheduler, host);
    }

    VoicePipelineState.ToolTts toolTtsState() {
        return toolTts.state();
    }

    void queueToolTts(String source, String text) {
        toolTts.queue(source, text);
    }

    boolean hasPendingToolTts() {
        return toolTts.hasPendingPlayback();
    }

    boolean hasActiveToolTts() {
        return toolTts.isPlaybackActive();
    }

    void startPendingToolTts(boolean force) {
        toolTts.startPending(force);
    }

    boolean onRealtimeStoppedBeforeToolTts() {
        return toolTts.onRealtimeStopped();
    }

    void stopToolTts(boolean resumeListening) {
        toolTts.stop(resumeListening);
    }
}
