package com.linkyun.her;

final class VoicePipelineManager {
    interface Host extends ToolTtsCoordinator.Host, RealtimeOutputCoordinator.Host {
    }

    private final ToolTtsCoordinator toolTts;
    private final RealtimeOutputCoordinator realtimeOutput;

    VoicePipelineManager(ToolTtsCoordinator.Scheduler scheduler, Host host) {
        toolTts = new ToolTtsCoordinator(scheduler, host);
        realtimeOutput = new RealtimeOutputCoordinator(host);
    }

    VoicePipelineState.ToolTts toolTtsState() {
        return toolTts.state();
    }

    VoicePipelineState.RealtimeOutput realtimeOutputState() {
        return realtimeOutput.state();
    }

    boolean isRealtimeOutputActive() {
        return realtimeOutput.isActive();
    }

    boolean shouldDiscardRealtimeAudio() {
        return realtimeOutput.shouldDiscardAudio();
    }

    void markRealtimeOutputInterrupted(boolean discardUntilDone) {
        realtimeOutput.markInterrupted(discardUntilDone);
    }

    void resetRealtimeOutput() {
        realtimeOutput.reset();
    }

    void onRealtimeOutputStarted(int sampleRate) {
        realtimeOutput.onStarted(sampleRate, toolTts.isPlaybackActive());
    }

    void onRealtimeOutputDone() {
        realtimeOutput.onDone();
    }

    void onRealtimeOutputStopped() {
        realtimeOutput.onStopped();
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
        realtimeOutput.onDone();
        return toolTts.onRealtimeStopped();
    }

    void stopToolTts(boolean resumeListening) {
        toolTts.stop(resumeListening);
    }
}
