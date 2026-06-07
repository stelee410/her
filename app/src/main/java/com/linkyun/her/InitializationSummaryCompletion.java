package com.linkyun.her;

import java.util.Date;

final class InitializationSummaryCompletion {
    private InitializationSummaryCompletion() {
    }

    static Result complete(String content,
            String transcript,
            String currentAgentName,
            String fallbackAgentName,
            String systemAgentName,
            String currentDisplayUserName,
            Date createdAt) {
        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                content, transcript, currentAgentName, fallbackAgentName, systemAgentName);
        return new Result(
                profile,
                profile.agentName,
                profile.displayUserName(currentDisplayUserName),
                profile.userMemory(createdAt),
                profile.agentMemory(createdAt));
    }

    static final class Result {
        final InitializationProfileResolver.Profile profile;
        final String agentName;
        final String userName;
        final String userMemory;
        final String agentMemory;

        Result(InitializationProfileResolver.Profile profile,
                String agentName,
                String userName,
                String userMemory,
                String agentMemory) {
            this.profile = profile;
            this.agentName = agentName;
            this.userName = userName;
            this.userMemory = userMemory;
            this.agentMemory = agentMemory;
        }
    }
}
