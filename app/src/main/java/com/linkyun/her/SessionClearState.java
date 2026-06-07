package com.linkyun.her;

final class SessionClearState {
    private SessionClearState() {
    }

    static RuntimeFields clearedRuntime() {
        return new RuntimeFields(false, null, "", "");
    }

    static ResetInitializationFields resetInitialization(String defaultTone) {
        return new ResetInitializationFields(
                "",
                "",
                "",
                "",
                "",
                defaultTone == null ? "" : defaultTone,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                clearedRuntime());
    }

    static Message sessionClearedMessage() {
        return new Message("session-cleared", "assistant", "这一轮已经清空。我们重新开始。");
    }

    static final class RuntimeFields {
        final boolean inputAudioOpen;
        final String pendingText;
        final String latestWeatherFact;
        final String latestNewsFact;

        RuntimeFields(boolean inputAudioOpen, String pendingText,
                String latestWeatherFact, String latestNewsFact) {
            this.inputAudioOpen = inputAudioOpen;
            this.pendingText = pendingText;
            this.latestWeatherFact = latestWeatherFact;
            this.latestNewsFact = latestNewsFact;
        }
    }

    static final class ResetInitializationFields {
        final String agentName;
        final String userName;
        final String userMemory;
        final String agentMemory;
        final String conversationMemory;
        final String dynamicTone;
        final boolean initialized;
        final boolean initializing;
        final boolean initPromptPending;
        final boolean initSummaryPending;
        final boolean summaryInProgress;
        final boolean ignoreNextInitTrigger;
        final int initUserTurns;
        final RuntimeFields runtime;

        ResetInitializationFields(String agentName,
                String userName,
                String userMemory,
                String agentMemory,
                String conversationMemory,
                String dynamicTone,
                boolean initialized,
                boolean initializing,
                boolean initPromptPending,
                boolean initSummaryPending,
                boolean summaryInProgress,
                boolean ignoreNextInitTrigger,
                int initUserTurns,
                RuntimeFields runtime) {
            this.agentName = agentName;
            this.userName = userName;
            this.userMemory = userMemory;
            this.agentMemory = agentMemory;
            this.conversationMemory = conversationMemory;
            this.dynamicTone = dynamicTone;
            this.initialized = initialized;
            this.initializing = initializing;
            this.initPromptPending = initPromptPending;
            this.initSummaryPending = initSummaryPending;
            this.summaryInProgress = summaryInProgress;
            this.ignoreNextInitTrigger = ignoreNextInitTrigger;
            this.initUserTurns = initUserTurns;
            this.runtime = runtime;
        }
    }
}
