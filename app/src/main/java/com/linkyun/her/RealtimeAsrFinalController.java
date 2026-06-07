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

    private final Host host;

    RealtimeAsrFinalController(Host host) {
        this.host = host;
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
        if (!text.isEmpty()) {
            host.markConversationInteraction();
            host.addUserMessage(text);
        }
        if (text.isEmpty()) {
            host.clearActiveAssistant();
            host.renderMessages();
            return;
        }
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
        if (!initializing) host.routeToolsInBackground(text);
    }
}
