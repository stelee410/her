package com.linkyun.her;

interface AudioFrameProcessor {
    byte[] process(byte[] buffer, int length);
    void reset();

    AudioFrameProcessor NONE = new AudioFrameProcessor() {
        @Override public byte[] process(byte[] buffer, int length) {
            byte[] out = new byte[Math.max(0, length)];
            if (buffer != null && length > 0) System.arraycopy(buffer, 0, out, 0, length);
            return out;
        }

        @Override public void reset() {
        }
    };
}
