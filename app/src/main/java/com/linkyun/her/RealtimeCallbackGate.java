package com.linkyun.her;

final class RealtimeCallbackGate {
    private int generation;

    synchronized int nextGeneration() {
        generation++;
        return generation;
    }

    synchronized void invalidate() {
        generation++;
    }

    synchronized boolean accepts(int callbackGeneration) {
        return callbackGeneration == generation;
    }
}
