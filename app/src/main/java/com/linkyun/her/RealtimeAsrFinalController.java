package com.linkyun.her;

final class RealtimeAsrFinalController {
    interface Host {
        boolean isInitializing();
        boolean isTextModeActive();
        void cancelAsrFinalTimeout();
        void clearIgnoreNextInitTrigger();
        void markConversationInteraction();
        void addUserMessage(String text);
        boolean recordInitializationAnswer(String text);
        boolean hasReachedInitializationTarget();
        void markInitSummaryPending();
        void scheduleInitializationContextUpdate();
        void clearActiveAssistant();
        void renderMessages();
        void finishInitializationWithSummary();
        boolean routeToolQuestion(String text, boolean realtimeMode);
        void routeToolsInBackground(String text);
        void setState(String nextState);
    }

    interface Clock {
        long nowMs();
    }

    private static final long DUPLICATE_FINAL_WINDOW_MS = 30_000;
    private final Host host;
    private final Clock clock;
    private String lastHandledText = "";
    private long lastHandledAtMs = Long.MIN_VALUE;

    RealtimeAsrFinalController(Host host) {
        this(host, System::currentTimeMillis);
    }

    RealtimeAsrFinalController(Host host, Clock clock) {
        this.host = host;
        this.clock = clock;
    }

    void onFinalText(String rawText) {
        host.cancelAsrFinalTimeout();
        if (host.isTextModeActive()) {
            host.clearActiveAssistant();
            host.renderMessages();
            host.setState("text_only");
            return;
        }
        boolean initializing = host.isInitializing();
        RealtimeAsrFinalText.Result asr = RealtimeAsrFinalText.classify(rawText, initializing);
        String text = asr.text;
        if (asr.hidden) {
            if (asr.resetIgnoreNextInitTrigger) host.clearIgnoreNextInitTrigger();
            return;
        }
        if (text.isEmpty()) {
            host.clearActiveAssistant();
            host.renderMessages();
            return;
        }
        if (isDuplicateFinal(text)) return;
        rememberFinal(text);
        host.markConversationInteraction();
        host.addUserMessage(text);
        if (initializing && host.recordInitializationAnswer(text)) {
            if (host.hasReachedInitializationTarget()) {
                host.markInitSummaryPending();
                host.clearActiveAssistant();
                host.renderMessages();
                host.finishInitializationWithSummary();
                return;
            }
            host.scheduleInitializationContextUpdate();
        }
        host.clearActiveAssistant();
        host.renderMessages();
        if (!initializing && host.routeToolQuestion(text, true)) return;
        if (!initializing) {
            host.routeToolsInBackground(text);
        }
    }

    private boolean isDuplicateFinal(String text) {
        long now = clock.nowMs();
        return text.equals(lastHandledText) && now - lastHandledAtMs <= DUPLICATE_FINAL_WINDOW_MS;
    }

    private void rememberFinal(String text) {
        lastHandledText = text;
        lastHandledAtMs = clock.nowMs();
    }
}
