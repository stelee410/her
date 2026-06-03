package com.linkyun.her;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PcmSampleBufferTest {
    @Test
    public void evenChunkPassesThrough() {
        PcmSampleBuffer buffer = new PcmSampleBuffer();
        byte[] target = new byte[5];

        int length = buffer.copyAligned(new byte[] {1, 2, 3, 4}, 4, target);

        assertEquals(4, length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, slice(target, length));
        assertFalse(buffer.hasPendingByte());
    }

    @Test
    public void oddChunkKeepsLastBytePending() {
        PcmSampleBuffer buffer = new PcmSampleBuffer();
        byte[] target = new byte[5];

        int length = buffer.copyAligned(new byte[] {1, 2, 3}, 3, target);

        assertEquals(2, length);
        assertArrayEquals(new byte[] {1, 2}, slice(target, length));
        assertTrue(buffer.hasPendingByte());
    }

    @Test
    public void pendingByteIsPrependedToNextChunk() {
        PcmSampleBuffer buffer = new PcmSampleBuffer();
        byte[] target = new byte[5];

        assertEquals(0, buffer.copyAligned(new byte[] {1}, 1, target));
        int length = buffer.copyAligned(new byte[] {2, 3, 4}, 3, target);

        assertEquals(4, length);
        assertArrayEquals(new byte[] {1, 2, 3, 4}, slice(target, length));
        assertFalse(buffer.hasPendingByte());
    }

    @Test
    public void pendingByteSurvivesWhenNextChunkStillLeavesOddTotal() {
        PcmSampleBuffer buffer = new PcmSampleBuffer();
        byte[] target = new byte[5];

        assertEquals(0, buffer.copyAligned(new byte[] {1}, 1, target));
        int length = buffer.copyAligned(new byte[] {2, 3}, 2, target);

        assertEquals(2, length);
        assertArrayEquals(new byte[] {1, 2}, slice(target, length));
        assertTrue(buffer.hasPendingByte());
    }

    private static byte[] slice(byte[] source, int length) {
        byte[] result = new byte[length];
        System.arraycopy(source, 0, result, 0, length);
        return result;
    }
}
