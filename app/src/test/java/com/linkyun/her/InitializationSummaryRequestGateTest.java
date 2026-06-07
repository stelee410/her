package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InitializationSummaryRequestGateTest {
    @Test
    public void newRequestInvalidatesEarlierRequest() {
        InitializationSummaryRequestGate gate = new InitializationSummaryRequestGate();

        int first = gate.start();
        int second = gate.start();

        assertFalse(gate.isCurrent(first));
        assertTrue(gate.isCurrent(second));
    }

    @Test
    public void invalidateCancelsCurrentRequest() {
        InitializationSummaryRequestGate gate = new InitializationSummaryRequestGate();

        int request = gate.start();
        gate.invalidate();

        assertFalse(gate.isCurrent(request));
    }
}
