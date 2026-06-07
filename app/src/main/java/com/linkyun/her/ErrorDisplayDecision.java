package com.linkyun.her;

final class ErrorDisplayDecision {
    final String message;
    final boolean updateInitializationLastTurn;
    final boolean appendAssistantMessage;
    final boolean renderMessages;
    final boolean updateVoiceHome;

    private ErrorDisplayDecision(String message,
            boolean updateInitializationLastTurn,
            boolean appendAssistantMessage,
            boolean renderMessages,
            boolean updateVoiceHome) {
        this.message = message;
        this.updateInitializationLastTurn = updateInitializationLastTurn;
        this.appendAssistantMessage = appendAssistantMessage;
        this.renderMessages = renderMessages;
        this.updateVoiceHome = updateVoiceHome;
    }

    static ErrorDisplayDecision decide(String message,
            boolean hasInitializationLastTurn,
            boolean hasMessageList,
            boolean hasVoiceLastTurn) {
        return new ErrorDisplayDecision(
                message == null ? "" : message,
                hasInitializationLastTurn,
                hasMessageList || hasVoiceLastTurn,
                hasMessageList,
                true);
    }
}
