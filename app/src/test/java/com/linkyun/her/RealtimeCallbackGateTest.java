package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeCallbackGateTest {
    @Test
    public void generationAcceptsOnlyCurrentCallbacks() {
        RealtimeCallbackGate gate = new RealtimeCallbackGate();

        int first = gate.nextGeneration();
        int second = gate.nextGeneration();

        assertFalse(gate.accepts(first));
        assertTrue(gate.accepts(second));
    }

    @Test
    public void invalidateDropsQueuedCallbacks() {
        RealtimeCallbackGate gate = new RealtimeCallbackGate();

        int generation = gate.nextGeneration();
        gate.invalidate();

        assertFalse(gate.accepts(generation));
    }
}
