package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public final class SessionClearStateTest {
    @Test
    public void clearedRuntimeDropsInputAndToolFacts() {
        SessionClearState.RuntimeFields runtime = SessionClearState.clearedRuntime();

        assertFalse(runtime.inputAudioOpen);
        assertNull(runtime.pendingText);
        assertEquals("", runtime.latestWeatherFact);
        assertEquals("", runtime.latestNewsFact);
    }

    @Test
    public void resetInitializationClearsProfileAndInitFlags() {
        SessionClearState.ResetInitializationFields fields =
                SessionClearState.resetInitialization("default tone");

        assertEquals("", fields.agentName);
        assertEquals("", fields.userName);
        assertEquals("", fields.userMemory);
        assertEquals("", fields.agentMemory);
        assertEquals("", fields.conversationMemory);
        assertEquals("default tone", fields.dynamicTone);
        assertFalse(fields.initialized);
        assertFalse(fields.initializing);
        assertFalse(fields.initPromptPending);
        assertFalse(fields.initSummaryPending);
        assertFalse(fields.summaryInProgress);
        assertFalse(fields.ignoreNextInitTrigger);
        assertEquals(0, fields.initUserTurns);
        assertFalse(fields.runtime.inputAudioOpen);
    }

    @Test
    public void sessionClearedMessageUsesStableAssistantText() {
        Message message = SessionClearState.sessionClearedMessage();

        assertEquals("session-cleared", message.id);
        assertEquals("assistant", message.role);
        assertEquals("这一轮已经清空。我们重新开始。", message.text);
    }
}
