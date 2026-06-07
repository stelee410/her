package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MemoryCompactionRequestGateTest {
    @Test
    public void newRequestInvalidatesEarlierRequest() {
        MemoryCompactionRequestGate gate = new MemoryCompactionRequestGate();

        int first = gate.start();
        int second = gate.start();

        assertFalse(gate.isCurrent(first));
        assertTrue(gate.isCurrent(second));
    }

    @Test
    public void invalidateCancelsCurrentRequest() {
        MemoryCompactionRequestGate gate = new MemoryCompactionRequestGate();

        int request = gate.start();
        gate.invalidate();

        assertFalse(gate.isCurrent(request));
    }
}
