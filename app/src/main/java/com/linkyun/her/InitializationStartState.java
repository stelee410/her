package com.linkyun.her;

final class InitializationStartState {
    private InitializationStartState() {
    }

    static Result from(String requestedAgentName, boolean boundHeadsetConnected) {
        String agentName = requestedAgentName == null ? "" : requestedAgentName.trim();
        return new Result(
                agentName,
                agentName.isEmpty() ? "initializing" : agentName,
                !agentName.isEmpty(),
                !boundHeadsetConnected);
    }

    static final class Result {
        final String agentName;
        final String sessionAgentName;
        final boolean shouldPersistAgentName;
        final boolean shouldEnterTextOnly;

        Result(String agentName, String sessionAgentName,
                boolean shouldPersistAgentName, boolean shouldEnterTextOnly) {
            this.agentName = agentName;
            this.sessionAgentName = sessionAgentName;
            this.shouldPersistAgentName = shouldPersistAgentName;
            this.shouldEnterTextOnly = shouldEnterTextOnly;
        }
    }
}
