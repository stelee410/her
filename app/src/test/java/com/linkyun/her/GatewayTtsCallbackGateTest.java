package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class GatewayTtsCallbackGateTest {
    @Test
    public void newRunInvalidatesPreviousRun() {
        GatewayTtsCallbackGate gate = new GatewayTtsCallbackGate();

        int first = gate.nextRun();
        int second = gate.nextRun();

        assertFalse(gate.isCurrent(first));
        assertTrue(gate.isCurrent(second));
    }

    @Test
    public void cancelInvalidatesCurrentRun() {
        GatewayTtsCallbackGate gate = new GatewayTtsCallbackGate();

        int run = gate.nextRun();
        gate.cancelCurrent();

        assertFalse(gate.isCurrent(run));
    }
}
