package com.linkyun.her;

final class StartupProfileNames {
    private StartupProfileNames() {
    }

    static String startupAgentName(String persistedAgentName, boolean initialized,
            String agentMemory, String systemAgentName) {
        String value = clean(persistedAgentName);
        if (initialized && value.isEmpty()) {
            value = clean(InitializationProfileResolver.extractAgentName(agentMemory));
            if (value.isEmpty()) value = clean(systemAgentName);
        }
        return value;
    }

    static String startupUserName(String persistedUserName, String userMemory) {
        String value = clean(persistedUserName);
        if (value.isEmpty()) value = clean(InitializationProfileResolver.extractUserName(userMemory));
        return value;
    }

    static String displayUserName(String userName) {
        String value = clean(userName);
        return value.isEmpty() ? "there" : value;
    }

    static String effectiveAgentName(String agentName, String systemAgentName) {
        String value = clean(agentName);
        return value.isEmpty() ? clean(systemAgentName) : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
