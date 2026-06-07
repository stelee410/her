package com.linkyun.her;

final class GatewayTtsCallbackGate {
    private int generation;

    synchronized int nextRun() {
        generation++;
        return generation;
    }

    synchronized void cancelCurrent() {
        generation++;
    }

    synchronized boolean isCurrent(int run) {
        return run == generation;
    }
}
