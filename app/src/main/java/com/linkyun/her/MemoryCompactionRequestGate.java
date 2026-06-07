package com.linkyun.her;

final class MemoryCompactionRequestGate {
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
