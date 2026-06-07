package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class MemoryCoordinatorTest {
    @Test
    public void ignoresInvalidOrNonChatMessages() {
        Host host = new Host();
        MemoryCoordinator coordinator = new MemoryCoordinator(2, 100, host);

        assertFalse(coordinator.persistMessage(null));
        assertFalse(coordinator.persistMessage(new Message("m1", "tool", "")));
        assertFalse(coordinator.persistMessage(new Message("m2", "tool", "card")));
        assertFalse(coordinator.persistMessage(new Message(null, "user", "hi")));
        assertFalse(coordinator.persistMessage(new Message(" ", "assistant", "hello")));

        assertEquals(0, host.store.events.size());
    }

    @Test
    public void persistsAssistantMessageOnce() {
        Host host = new Host();
        MemoryCoordinator coordinator = new MemoryCoordinator(2, 100, host);
        Message message = new Message("a1", "assistant", " hello ");

        assertTrue(coordinator.persistMessage(message));
        assertFalse(coordinator.persistMessage(message));

        assertEquals("insert:1:assistant:hello", host.store.events.get(0));
        assertEquals(1, host.store.insertCount);
    }

    @Test
    public void userMessageRefreshesRelevantMemoryAndContext() {
        Host host = new Host();
        host.store.relevant = "memory";
        MemoryCoordinator coordinator = new MemoryCoordinator(2, 100, host);

        assertTrue(coordinator.persistMessage(new Message("u1", "user", " hi ")));

        assertEquals("insert:1:user:hi", host.store.events.get(0));
        assertEquals("userPersisted:hi:memory", host.events.get(0));
        assertEquals("context", host.events.get(1));
    }

    @Test
    public void skipsPersistenceWhenSessionNotReady() {
        Host host = new Host();
        host.initialized = false;
        MemoryCoordinator coordinator = new MemoryCoordinator(2, 100, host);

        assertFalse(coordinator.persistMessage(new Message("u1", "user", "hi")));

        assertEquals(0, host.store.events.size());
    }

    @Test
    public void triggersCompactWhenStoreReturnsChunk() {
        Host host = new Host();
        host.store.chunk = new MemoryChunk(1, 2, "user: hi");
        MemoryCoordinator coordinator = new MemoryCoordinator(2, 100, host);

        assertTrue(coordinator.persistMessage(new Message("u1", "user", "hi")));

        assertEquals("compactInProgress", host.events.get(2));
        assertEquals("compact:2", host.events.get(3));
    }

    @Test
    public void clearPersistedIdsAllowsSessionResetToReuseMessageIds() {
        Host host = new Host();
        MemoryCoordinator coordinator = new MemoryCoordinator(2, 100, host);
        Message message = new Message("same", "assistant", "one");

        assertTrue(coordinator.persistMessage(message));
        coordinator.clearPersistedMessageIds();
        assertTrue(coordinator.persistMessage(message));

        assertEquals(2, host.store.insertCount);
    }

    private static final class Host implements MemoryCoordinator.Host {
        final Store store = new Store();
        final List<String> events = new ArrayList<>();
        boolean initialized = true;
        boolean initializing;
        boolean summary;
        boolean compact;
        long sessionId = 1;

        @Override public boolean isInitialized() { return initialized; }
        @Override public boolean isInitializing() { return initializing; }
        @Override public boolean isSummaryInProgress() { return summary; }
        @Override public boolean isCompactInProgress() { return compact; }
        @Override public long sessionId() { return sessionId; }
        @Override public MemoryCoordinator.Store memoryStore() { return store; }
        @Override public void onUserMessagePersisted(String text, String relevantMemory) {
            events.add("userPersisted:" + text + ":" + relevantMemory);
        }
        @Override public void applyContextUpdateForNextTurn() { events.add("context"); }
        @Override public void markCompactInProgress() {
            compact = true;
            events.add("compactInProgress");
        }
        @Override public void compactConversation(MemoryChunk chunk) { events.add("compact:" + chunk.lastId); }
    }

    private static final class Store implements MemoryCoordinator.Store {
        final List<String> events = new ArrayList<>();
        String relevant = "";
        MemoryChunk chunk;
        int insertCount;

        @Override public void insertMessage(long sessionId, String role, String content) {
            insertCount++;
            events.add("insert:" + sessionId + ":" + role + ":" + content);
        }

        @Override public String relevantMemory(String query) {
            return relevant;
        }

        @Override public MemoryChunk unsummarizedChunk(long sessionId, int minCount, int minChars) {
            return chunk;
        }
    }
}
