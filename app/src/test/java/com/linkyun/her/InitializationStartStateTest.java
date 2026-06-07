package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InitializationStartStateTest {
    @Test
    public void blankNameUsesInitializingSessionAndClearsPreference() {
        InitializationStartState.Result result = InitializationStartState.from("   ", true);

        assertEquals("", result.agentName);
        assertEquals("initializing", result.sessionAgentName);
        assertFalse(result.shouldPersistAgentName);
        assertFalse(result.shouldEnterTextOnly);
    }

    @Test
    public void providedNameIsTrimmedPersistedAndUsedForSession() {
        InitializationStartState.Result result = InitializationStartState.from(" Mira ", true);

        assertEquals("Mira", result.agentName);
        assertEquals("Mira", result.sessionAgentName);
        assertTrue(result.shouldPersistAgentName);
        assertFalse(result.shouldEnterTextOnly);
    }

    @Test
    public void missingHeadsetStartsInitializationInTextOnlyState() {
        InitializationStartState.Result result = InitializationStartState.from(null, false);

        assertEquals("", result.agentName);
        assertEquals("initializing", result.sessionAgentName);
        assertTrue(result.shouldEnterTextOnly);
    }
}
