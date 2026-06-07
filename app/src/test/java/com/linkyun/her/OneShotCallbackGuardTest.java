package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class OneShotCallbackGuardTest {
    @Test
    public void firstCompletionWins() {
        OneShotCallbackGuard guard = new OneShotCallbackGuard();

        assertTrue(guard.tryComplete());
        assertFalse(guard.tryComplete());
        assertFalse(guard.tryComplete());
    }
}
