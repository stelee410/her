package com.linkyun.her;

final class NoiseSuppressingAudioProcessor implements AudioFrameProcessor {
    private static final float HIGH_PASS = 0.94f;
    private static final int MIN_GATE = 160;
    private static final float CLOSED_GATE_GAIN = 0.35f;
    private static final float OPEN_GATE_GAIN = 1.0f;
    private float previousInput;
    private float previousOutput;
    private float noiseFloor = MIN_GATE;
    private float gateGain = OPEN_GATE_GAIN;

    @Override public synchronized byte[] process(byte[] buffer, int length) {
        if (buffer == null || length <= 0) return new byte[0];
        int safeLength = length - (length % 2);
        byte[] out = new byte[safeLength];
        int level = VoiceActivityDetector.averageAbsPcm16(buffer, safeLength);
        boolean likelyNoise = level < Math.max(MIN_GATE, noiseFloor * 1.45f + 60);
        if (likelyNoise) {
            noiseFloor = noiseFloor * 0.96f + level * 0.04f;
            gateGain = gateGain * 0.72f + CLOSED_GATE_GAIN * 0.28f;
        } else {
            gateGain = gateGain * 0.65f + OPEN_GATE_GAIN * 0.35f;
        }
        for (int i = 0; i + 1 < safeLength; i += 2) {
            short sample = (short) ((buffer[i] & 0xff) | (buffer[i + 1] << 8));
            float filtered = sample - previousInput + HIGH_PASS * previousOutput;
            previousInput = sample;
            previousOutput = filtered;
            int cleaned = clamp16(Math.round(filtered * gateGain));
            out[i] = (byte) (cleaned & 0xff);
            out[i + 1] = (byte) ((cleaned >> 8) & 0xff);
        }
        return out;
    }

    @Override public synchronized void reset() {
        previousInput = 0;
        previousOutput = 0;
        noiseFloor = MIN_GATE;
        gateGain = OPEN_GATE_GAIN;
    }

    private static int clamp16(int value) {
        if (value > Short.MAX_VALUE) return Short.MAX_VALUE;
        if (value < Short.MIN_VALUE) return Short.MIN_VALUE;
        return value;
    }
}
