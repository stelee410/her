package com.linkyun.her;

final class MemoryChunk {
    final long firstId;
    final long lastId;
    final String transcript;

    MemoryChunk(long firstId, long lastId, String transcript) {
        this.firstId = firstId;
        this.lastId = lastId;
        this.transcript = transcript;
    }
}
