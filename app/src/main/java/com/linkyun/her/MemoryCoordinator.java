package com.linkyun.her;

import java.util.HashSet;
import java.util.Set;

final class MemoryCoordinator {
    interface Store {
        void insertMessage(long sessionId, String role, String content);
        String relevantMemory(String query);
        MemoryChunk unsummarizedChunk(long sessionId, int minCount, int minChars);
    }

    interface Host {
        boolean isInitialized();
        boolean isInitializing();
        boolean isSummaryInProgress();
        boolean isCompactInProgress();
        long sessionId();
        Store memoryStore();
        void onUserMessagePersisted(String text, String relevantMemory);
        void applyContextUpdateForNextTurn();
        void markCompactInProgress();
        void compactConversation(MemoryChunk chunk);
    }

    private final int compactMessageThreshold;
    private final int compactCharThreshold;
    private final Host host;
    private final Set<String> persistedMessageIds = new HashSet<>();

    MemoryCoordinator(int compactMessageThreshold, int compactCharThreshold, Host host) {
        this.compactMessageThreshold = compactMessageThreshold;
        this.compactCharThreshold = compactCharThreshold;
        this.host = host;
    }

    boolean persistMessage(Message message) {
        if (message == null || message.text == null || message.text.trim().isEmpty()) return false;
        if (message.id == null || message.id.trim().isEmpty()) return false;
        if (!host.isInitialized() || host.isInitializing()) return false;
        long sessionId = host.sessionId();
        Store store = host.memoryStore();
        if (sessionId <= 0 || store == null) return false;
        if (!"user".equals(message.role) && !"assistant".equals(message.role)) return false;
        if (!persistedMessageIds.add(message.id)) return false;
        String text = message.text.trim();
        store.insertMessage(sessionId, message.role, text);
        if ("user".equals(message.role)) {
            String relevantMemory = store.relevantMemory(text);
            host.onUserMessagePersisted(text, relevantMemory);
            host.applyContextUpdateForNextTurn();
        }
        maybeCompactMemory();
        return true;
    }

    void clearPersistedMessageIds() {
        persistedMessageIds.clear();
    }

    private void maybeCompactMemory() {
        if (host.isCompactInProgress() || host.isSummaryInProgress() || host.isInitializing()) return;
        long sessionId = host.sessionId();
        Store store = host.memoryStore();
        if (store == null || sessionId <= 0) return;
        MemoryChunk chunk = store.unsummarizedChunk(sessionId, compactMessageThreshold, compactCharThreshold);
        if (chunk == null) return;
        host.markCompactInProgress();
        host.compactConversation(chunk);
    }
}
