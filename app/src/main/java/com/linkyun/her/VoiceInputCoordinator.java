package com.linkyun.her;

final class VoiceInputCoordinator {
    interface Scheduler {
        void postDelayed(Runnable runnable, long delayMs);
    }

    interface Host {
        boolean isTextModeActive();
        boolean isInputActive();
        boolean isRealtimeOpen();
        boolean isBoundHeadsetConnected();
        boolean hasRecordPermission();
        boolean hasActiveToolTtsPlayback();
        boolean isReadyForContinuousListening();
        boolean isVoiceSurfaceActive();
        void requestRecordPermission();
        void connectRealtime();
        void prepareInputStart(String interruptReason);
        void startInputAudio();
        void stopInputAudio(String nextState);
        void setState(String nextState);
        void showHeadsetPrompt();
        void logVoiceInput(String message);
    }

    private final Scheduler scheduler;
    private final Host host;
    private final boolean continuousConversation;
    private int continuousSeq;
    private boolean pendingStart;
    private String pendingInterruptReason;

    VoiceInputCoordinator(Scheduler scheduler, Host host, boolean continuousConversation) {
        this.scheduler = scheduler;
        this.host = host;
        this.continuousConversation = continuousConversation;
    }

    boolean hasPendingStart() {
        return pendingStart;
    }

    void enterTextMode() {
        cancelContinuousListening();
        clearPendingStart();
        if (host.isInputActive()) {
            host.stopInputAudio("text_only");
        } else {
            host.setState("text_only");
        }
    }

    void requestStart(boolean requestPermission, String interruptReason, boolean showHeadsetPrompt) {
        if (host.isTextModeActive()) {
            clearPendingStart();
            return;
        }
        if (host.isInputActive()) return;
        if (!host.isBoundHeadsetConnected()) {
            if (showHeadsetPrompt) host.showHeadsetPrompt();
            clearPendingStart();
            return;
        }
        if (!host.hasRecordPermission()) {
            clearPendingStart();
            if (requestPermission) {
                pendingStart = true;
                pendingInterruptReason = interruptReason;
                host.requestRecordPermission();
            }
            return;
        }
        pendingStart = true;
        pendingInterruptReason = interruptReason;
        if (host.isRealtimeOpen()) {
            startPendingNow();
        } else {
            host.connectRealtime();
        }
    }

    void onRecordPermissionGranted() {
        if (!pendingStart) return;
        if (host.isTextModeActive()) {
            clearPendingStart();
            return;
        }
        if (host.isRealtimeOpen()) {
            startPendingNow();
        } else {
            host.connectRealtime();
        }
    }

    void onRecordPermissionDenied() {
        clearPendingStart();
    }

    boolean onRealtimeReady() {
        if (!pendingStart) return false;
        startPendingNow();
        return true;
    }

    void resumeAfterToolTts(long delayMs) {
        if (!continuousConversation) return;
        scheduler.postDelayed(() -> {
            if (!host.isVoiceSurfaceActive()) return;
            if (host.isInputActive()) return;
            requestStart(false, null, false);
        }, delayMs);
    }

    void startContinuousListening() {
        if (!continuousConversation) return;
        if (host.isTextModeActive()) return;
        if (host.hasActiveToolTtsPlayback()) return;
        if (host.isInputActive() || !host.isRealtimeOpen()) return;
        if (!host.isBoundHeadsetConnected()) return;
        if (!host.hasRecordPermission()) return;
        cancelContinuousListening();
        host.startInputAudio();
    }

    void scheduleContinuousListening(long delayMs) {
        if (!continuousConversation) return;
        int seq = ++continuousSeq;
        scheduler.postDelayed(() -> {
            if (seq != continuousSeq) return;
            if (host.isTextModeActive()) return;
            if (host.hasActiveToolTtsPlayback()) return;
            if (host.isReadyForContinuousListening() && !host.isInputActive()) {
                startContinuousListening();
            }
        }, delayMs);
    }

    void cancelContinuousListening() {
        continuousSeq++;
    }

    void clearPendingStart() {
        pendingStart = false;
        pendingInterruptReason = null;
    }

    private void startPendingNow() {
        if (host.isTextModeActive()) {
            clearPendingStart();
            return;
        }
        if (host.isInputActive()) {
            clearPendingStart();
            return;
        }
        if (!host.isBoundHeadsetConnected()) {
            clearPendingStart();
            return;
        }
        if (!host.hasRecordPermission()) return;
        String interruptReason = pendingInterruptReason;
        clearPendingStart();
        host.logVoiceInput("start input interruptReason=" + interruptReason);
        if (interruptReason != null && !interruptReason.trim().isEmpty()) {
            host.prepareInputStart(interruptReason);
        }
        host.startInputAudio();
    }
}
