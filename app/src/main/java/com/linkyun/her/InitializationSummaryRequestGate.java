package com.linkyun.her;

final class InitializationSummaryRequestGate {
    private int generation;

    int start() {
        return ++generation;
    }

    void invalidate() {
        generation++;
    }

    boolean isCurrent(int requestId) {
        return requestId == generation;
    }
}
