package com.linkyun.her;

final class TextInputController {
    interface Host {
        boolean isSummaryInProgress();
        boolean isInitializing();
        void markConversationInteraction();
        void addUserMessage(String text);
        boolean recordInitializationAnswer(String text);
        boolean hasReachedInitializationTarget();
        void scheduleInitializationContextUpdate();
        void clearActiveAssistant();
        void renderMessages();
        void finishInitializationWithSummary();
        boolean routeToolQuestion(String text, boolean realtimeMode);
        void sendTextWithAgentLLM(String text);
    }

    private final Host host;

    TextInputController(Host host) {
        this.host = host;
    }

    boolean sendText(String text) {
        if (host.isSummaryInProgress()) return false;
        String clean = text == null ? "" : text.trim();
        if (clean.isEmpty()) return false;
        host.markConversationInteraction();
        host.addUserMessage(clean);
        if (host.isInitializing()) {
            if (host.recordInitializationAnswer(clean) && host.hasReachedInitializationTarget()) {
                host.renderMessages();
                host.finishInitializationWithSummary();
                return true;
            }
            host.scheduleInitializationContextUpdate();
        }
        host.clearActiveAssistant();
        host.renderMessages();
        if (!host.isInitializing() && host.routeToolQuestion(clean, false)) return true;
        host.sendTextWithAgentLLM(clean);
        return true;
    }
}
