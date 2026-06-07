package com.linkyun.her;

final class RealtimeErrorRecoveryController {
    static final int MAX_RETRIES = 2;
    static final long RETRY_DELAY_MS = 1200;

    interface Host {
        boolean isInitializing();
        boolean isTextModeActive();
        void logInitializationDegraded(String reason);
        void stopMic();
        void markInputAudioClosed();
        void clearVoiceInputRequests();
        void clearInitPromptPending();
        void stopRealtimeAudio();
        void closeRealtime();
        boolean isRealtimeOpen();
        void connectRealtime();
        void postDelayed(Runnable runnable, long delayMs);
        void setState(String nextState);
        void updateInitProgress();
        void toastError(String message);
    }

    private final Host host;
    private int retryCount;
    private int reconnectGeneration;

    RealtimeErrorRecoveryController(Host host) {
        this.host = host;
    }

    void resetRetryCount() {
        retryCount = 0;
        reconnectGeneration++;
    }

    int retryCount() {
        return retryCount;
    }

    void onTransportError(String message) {
        if (host.isTextModeActive()) {
            preserveTextMode();
            return;
        }
        if (host.isInitializing()) {
            degradeInitialization(message);
            return;
        }
        host.setState("error");
        host.toastError(message);
    }

    void retry(String reason) {
        if (host.isTextModeActive()) {
            preserveTextMode();
            return;
        }
        if (host.isInitializing()) {
            degradeInitialization(reason);
            return;
        }
        retryCount++;
        if (retryCount > MAX_RETRIES) {
            reconnectGeneration++;
            host.toastError("语音交互模型暂时不可用，已切到文字聊天：" + reason);
            host.setState("text_only");
            return;
        }
        int generation = ++reconnectGeneration;
        host.toastError("语音交互模型连接超时，正在重试 " + retryCount + "/" + MAX_RETRIES + "...");
        host.setState("connecting");
        host.closeRealtime();
        host.postDelayed(() -> {
            if (generation == reconnectGeneration && !host.isRealtimeOpen()) host.connectRealtime();
        }, RETRY_DELAY_MS);
    }

    private void preserveTextMode() {
        retryCount = 0;
        reconnectGeneration++;
        host.setState("text_only");
    }

    void degradeInitialization(String reason) {
        host.logInitializationDegraded(reason);
        host.stopMic();
        host.markInputAudioClosed();
        host.clearVoiceInputRequests();
        host.clearInitPromptPending();
        retryCount = 0;
        reconnectGeneration++;
        host.stopRealtimeAudio();
        host.closeRealtime();
        host.setState("text_only");
        host.updateInitProgress();
        host.toastError("语音服务暂时不可用，我们先用文字继续初始化。");
    }
}
