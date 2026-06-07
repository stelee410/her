package com.linkyun.her;

final class ToolTtsCoordinator {
    interface Scheduler {
        void postDelayed(Runnable runnable, long delayMs);
    }

    interface Host {
        boolean isTextModeActive();
        boolean isVoiceSurfaceActive();
        boolean isRealtimePlaybackActive();
        boolean shouldDeferStart();
        boolean isSpeakingState();
        void logToolTts(String message);
        void interruptRealtimePlayback(String reason, boolean discardUntilDone);
        void prepareToolTtsPlayback();
        void playToolTts(String id, String text, PlaybackListener listener);
        void onToolTtsStarted(String id, String text);
        void onToolTtsFinished(String id);
        void resumeListeningAfterToolTts(long delayMs);
    }

    interface PlaybackListener {
        void onStarted(String id, String text);
        void onCompleted(String id);
        void onError(String id, String message);
    }

    private static final long DEFERRED_START_MS = 3200;
    private static final long FORCE_START_AFTER_INTERRUPT_MS = 220;
    private static final long RESUME_AFTER_PLAYBACK_MS = 350;

    private final Scheduler scheduler;
    private final Host host;
    private VoicePipelineState.ToolTts state = VoicePipelineState.ToolTts.IDLE;
    private String pendingId;
    private String pendingText;
    private String activePlaybackId;

    ToolTtsCoordinator(Scheduler scheduler, Host host) {
        this.scheduler = scheduler;
        this.host = host;
    }

    VoicePipelineState.ToolTts state() {
        return state;
    }

    boolean hasPendingPlayback() {
        return pendingText != null && !pendingText.trim().isEmpty();
    }

    boolean isPlaybackActive() {
        return state == VoicePipelineState.ToolTts.PLAYING ||
                state == VoicePipelineState.ToolTts.REQUESTING ||
                state == VoicePipelineState.ToolTts.WAITING_REALTIME_STOP ||
                hasPendingPlayback();
    }

    void queue(String source, String text) {
        if (text == null || text.trim().isEmpty()) return;
        if (host.isTextModeActive()) return;
        if (!host.isVoiceSurfaceActive()) return;
        pendingId = source + "-" + System.currentTimeMillis();
        pendingText = text.trim();
        String queuedId = pendingId;
        state = VoicePipelineState.ToolTts.QUEUED;
        host.logToolTts("queue source=" + source + " len=" + pendingText.length());
        if (host.shouldDeferStart()) {
            scheduler.postDelayed(() -> {
                if (isPendingCallback(queuedId) && !host.isSpeakingState()) {
                    startPending(false);
                }
            }, DEFERRED_START_MS);
            return;
        }
        startPending(false);
    }

    void startPending(boolean force) {
        if (!hasPendingPlayback()) return;
        if (host.isTextModeActive()) {
            discardPendingForTextMode();
            return;
        }
        if (!host.isVoiceSurfaceActive()) {
            discardPendingForInactiveSurface();
            return;
        }
        if (!force && host.isRealtimePlaybackActive()) {
            state = VoicePipelineState.ToolTts.WAITING_REALTIME_STOP;
            host.logToolTts("waiting for realtime stop");
            host.interruptRealtimePlayback("tool_tts_playback", true);
            String interruptedId = pendingId;
            scheduler.postDelayed(() -> {
                if (isPendingCallback(interruptedId)) {
                    host.logToolTts("force start after realtime interrupt");
                    startPending(true);
                }
            }, FORCE_START_AFTER_INTERRUPT_MS);
            return;
        }
        state = VoicePipelineState.ToolTts.REQUESTING;
        String id = pendingId;
        String text = pendingText;
        pendingId = null;
        pendingText = null;
        if (id == null) id = "tool-" + System.currentTimeMillis();
        activePlaybackId = id;
        host.prepareToolTtsPlayback();
        host.playToolTts(id, text, new PlaybackListener() {
            @Override public void onStarted(String startedId, String spokenText) {
                if (!isActiveCallback(startedId)) return;
                if (host.isTextModeActive()) {
                    activePlaybackId = null;
                    state = VoicePipelineState.ToolTts.IDLE;
                    host.onToolTtsFinished(startedId);
                    return;
                }
                if (!host.isVoiceSurfaceActive()) {
                    activePlaybackId = null;
                    state = VoicePipelineState.ToolTts.IDLE;
                    host.onToolTtsFinished(startedId);
                    return;
                }
                state = VoicePipelineState.ToolTts.PLAYING;
                host.onToolTtsStarted(startedId, spokenText);
            }

            @Override public void onCompleted(String completedId) {
                finish(completedId);
            }

            @Override public void onError(String failedId, String message) {
                if (!isActiveCallback(failedId)) return;
                host.logToolTts("failed: " + message);
                finish(failedId);
            }
        });
    }

    boolean onRealtimeStopped() {
        if (!hasPendingPlayback()) {
            if (state == VoicePipelineState.ToolTts.WAITING_REALTIME_STOP) {
                state = VoicePipelineState.ToolTts.IDLE;
            }
            return false;
        }
        startPending(false);
        return true;
    }

    void stop(boolean resumeListening) {
        pendingId = null;
        pendingText = null;
        activePlaybackId = null;
        state = VoicePipelineState.ToolTts.IDLE;
        host.onToolTtsFinished(null);
        if (resumeListening && host.isVoiceSurfaceActive()) host.resumeListeningAfterToolTts(80);
    }

    private void finish(String id) {
        if (!isActiveCallback(id)) return;
        activePlaybackId = null;
        state = VoicePipelineState.ToolTts.IDLE;
        host.onToolTtsFinished(id);
        if (!host.isTextModeActive() && host.isVoiceSurfaceActive()) {
            host.resumeListeningAfterToolTts(RESUME_AFTER_PLAYBACK_MS);
        }
    }

    private void discardPendingForTextMode() {
        pendingId = null;
        pendingText = null;
        activePlaybackId = null;
        state = VoicePipelineState.ToolTts.IDLE;
        host.onToolTtsFinished(null);
    }

    private void discardPendingForInactiveSurface() {
        pendingId = null;
        pendingText = null;
        activePlaybackId = null;
        state = VoicePipelineState.ToolTts.IDLE;
        host.onToolTtsFinished(null);
    }

    private boolean isActiveCallback(String id) {
        return activePlaybackId != null && activePlaybackId.equals(id);
    }

    private boolean isPendingCallback(String id) {
        return pendingId != null && pendingId.equals(id) && hasPendingPlayback();
    }
}
