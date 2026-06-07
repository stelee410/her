package com.linkyun.her;

final class InitializationAgentNameDecision {
    final String agentName;
    final boolean selectedFallback;
    final boolean persistAgentName;
    final boolean updateSessionAgentName;

    private InitializationAgentNameDecision(String agentName,
            boolean selectedFallback,
            boolean persistAgentName,
            boolean updateSessionAgentName) {
        this.agentName = agentName;
        this.selectedFallback = selectedFallback;
        this.persistAgentName = persistAgentName;
        this.updateSessionAgentName = updateSessionAgentName;
    }

    static InitializationAgentNameDecision ensure(String currentAgentName,
            String fallbackAgentName,
            boolean hasWritableSession) {
        if (!requiresFallback(currentAgentName)) {
            return new InitializationAgentNameDecision(
                    currentAgentName,
                    false,
                    false,
                    false);
        }
        String fallback = fallbackAgentName == null ? "" : fallbackAgentName.trim();
        return new InitializationAgentNameDecision(
                fallback,
                true,
                true,
                hasWritableSession && !fallback.isEmpty());
    }

    static boolean requiresFallback(String currentAgentName) {
        String current = currentAgentName == null ? "" : currentAgentName.trim();
        return current.isEmpty();
    }
}
