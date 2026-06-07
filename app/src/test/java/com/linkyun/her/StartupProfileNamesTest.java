package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class StartupProfileNamesTest {
    @Test
    public void initializedAgentNameFallsBackToAgentMemoryThenSystemName() {
        assertEquals("Mira", StartupProfileNames.startupAgentName(
                "   ", true, "- Agent name: Mira\n", "Doris"));
        assertEquals("Doris", StartupProfileNames.startupAgentName(
                null, true, "", "Doris"));
    }

    @Test
    public void uninitializedAgentNameKeepsCleanPersistedValue() {
        assertEquals("Clara", StartupProfileNames.startupAgentName(
                " Clara ", false, "- Agent name: Mira\n", "Doris"));
        assertEquals("", StartupProfileNames.startupAgentName(
                null, false, "- Agent name: Mira\n", "Doris"));
    }

    @Test
    public void userNameFallsBackToUserMemory() {
        assertEquals("史蒂芬", StartupProfileNames.startupUserName(
                "   ", "# user.md\n- 姓名/称呼：史蒂芬\n"));
        assertEquals("小林", StartupProfileNames.startupUserName(
                " 小林 ", "# user.md\n- 姓名/称呼：史蒂芬\n"));
        assertEquals("", StartupProfileNames.startupUserName(null, ""));
    }

    @Test
    public void displayAndEffectiveNamesUseStableFallbacks() {
        assertEquals("there", StartupProfileNames.displayUserName(null));
        assertEquals("there", StartupProfileNames.displayUserName("   "));
        assertEquals("史蒂芬", StartupProfileNames.displayUserName(" 史蒂芬 "));

        assertEquals("Doris", StartupProfileNames.effectiveAgentName(null, "Doris"));
        assertEquals("Mira", StartupProfileNames.effectiveAgentName(" Mira ", "Doris"));
    }
}
