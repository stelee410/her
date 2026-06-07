package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InitializationAgentNameDecisionTest {
    @Test
    public void keepsExistingNonBlankNameWithoutPersistenceWork() {
        InitializationAgentNameDecision decision =
                InitializationAgentNameDecision.ensure("  Doris  ", "Ava", true);

        assertEquals("  Doris  ", decision.agentName);
        assertFalse(decision.selectedFallback);
        assertFalse(decision.persistAgentName);
        assertFalse(decision.updateSessionAgentName);
    }

    @Test
    public void selectsFallbackAndUpdatesWritableSessionWhenNameIsBlank() {
        InitializationAgentNameDecision decision =
                InitializationAgentNameDecision.ensure("  ", "  Ava  ", true);

        assertEquals("Ava", decision.agentName);
        assertTrue(decision.selectedFallback);
        assertTrue(decision.persistAgentName);
        assertTrue(decision.updateSessionAgentName);
    }

    @Test
    public void blankFallbackIsPersistedButNotWrittenToSession() {
        InitializationAgentNameDecision decision =
                InitializationAgentNameDecision.ensure(null, "  ", true);

        assertEquals("", decision.agentName);
        assertTrue(decision.selectedFallback);
        assertTrue(decision.persistAgentName);
        assertFalse(decision.updateSessionAgentName);
    }

    @Test
    public void selectedFallbackSkipsSessionUpdateWhenNoSessionExists() {
        InitializationAgentNameDecision decision =
                InitializationAgentNameDecision.ensure(null, "Nora", false);

        assertEquals("Nora", decision.agentName);
        assertTrue(decision.selectedFallback);
        assertTrue(decision.persistAgentName);
        assertFalse(decision.updateSessionAgentName);
    }

    @Test
    public void requiresFallbackOnlyForBlankNames() {
        assertTrue(InitializationAgentNameDecision.requiresFallback(null));
        assertTrue(InitializationAgentNameDecision.requiresFallback("  "));
        assertFalse(InitializationAgentNameDecision.requiresFallback("Doris"));
        assertFalse(InitializationAgentNameDecision.requiresFallback("  Doris  "));
    }
}
