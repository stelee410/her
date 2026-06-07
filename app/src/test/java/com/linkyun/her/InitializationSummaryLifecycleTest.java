package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InitializationSummaryLifecycleTest {
    @Test
    public void startsOnlyWhenNoSummaryIsRunning() {
        assertTrue(InitializationSummaryLifecycle.shouldStart(false));
        assertFalse(InitializationSummaryLifecycle.shouldStart(true));
    }

    @Test
    public void activeMarksSummaryInProgressAndPending() {
        InitializationSummaryLifecycle.Flags flags = InitializationSummaryLifecycle.active();

        assertTrue(flags.summaryInProgress);
        assertTrue(flags.initSummaryPending);
    }

    @Test
    public void clearedResetsInProgressAndPendingTogether() {
        InitializationSummaryLifecycle.Flags flags = InitializationSummaryLifecycle.cleared();

        assertFalse(flags.summaryInProgress);
        assertFalse(flags.initSummaryPending);
    }
}
