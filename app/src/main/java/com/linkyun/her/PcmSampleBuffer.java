package com.linkyun.her;

final class PcmSampleBuffer {
    private byte pendingByte;
    private boolean hasPendingByte;

    int copyAligned(byte[] source, int length, byte[] target) {
        if (source == null || target == null || length <= 0) {
            return 0;
        }
        int outputOffset = 0;
        if (hasPendingByte) {
            target[outputOffset++] = pendingByte;
            hasPendingByte = false;
        }

        int sourceBytes = length;
        if (((outputOffset + sourceBytes) & 1) != 0) {
            sourceBytes--;
            pendingByte = source[length - 1];
            hasPendingByte = true;
        }
        if (sourceBytes > 0) {
            System.arraycopy(source, 0, target, outputOffset, sourceBytes);
            outputOffset += sourceBytes;
        }
        return outputOffset;
    }

    boolean hasPendingByte() {
        return hasPendingByte;
    }
}
