package com.linkyun.her;

final class VoiceActivityDetector {
    static final int DEFAULT_SPEECH_THRESHOLD = 520;
    private static final float DEFAULT_NOISE_MULTIPLIER = 2.5f;
    private static final int DEFAULT_NOISE_OFFSET = 120;
    private static final float DEFAULT_INITIAL_NOISE_FLOOR = 180;
    private static final int MIN_SPEECH_FRAMES = 2;
    private static final int SILENCE_FRAMES_TO_END = 45;
    private static final int MIN_FRAMES_BEFORE_END = 24;

    static final class Result {
        final int level;
        final int visualLevel;
        final boolean speech;
        final boolean shouldEndInput;

        Result(int level, int visualLevel, boolean speech, boolean shouldEndInput) {
            this.level = level;
            this.visualLevel = visualLevel;
            this.speech = speech;
            this.shouldEndInput = shouldEndInput;
        }
    }

    private boolean speechStarted;
    private int silenceFrames;
    private int speechFrames;
    private int frames;
    private int speechThreshold;
    private float noiseMultiplier;
    private int noiseOffset;
    private float initialNoiseFloor;
    private float noiseFloor;

    VoiceActivityDetector() {
        this(DEFAULT_SPEECH_THRESHOLD, DEFAULT_NOISE_MULTIPLIER, DEFAULT_NOISE_OFFSET,
                DEFAULT_INITIAL_NOISE_FLOOR);
    }

    VoiceActivityDetector(int speechThreshold, float noiseMultiplier, int noiseOffset,
            float initialNoiseFloor) {
        configure(speechThreshold, noiseMultiplier, noiseOffset, initialNoiseFloor);
    }

    void configureDefault() {
        configure(DEFAULT_SPEECH_THRESHOLD, DEFAULT_NOISE_MULTIPLIER, DEFAULT_NOISE_OFFSET,
                DEFAULT_INITIAL_NOISE_FLOOR);
    }

    void configure(int speechThreshold, float noiseMultiplier, int noiseOffset,
            float initialNoiseFloor) {
        this.speechThreshold = speechThreshold;
        this.noiseMultiplier = noiseMultiplier;
        this.noiseOffset = noiseOffset;
        this.initialNoiseFloor = initialNoiseFloor;
        reset();
    }

    void reset() {
        speechStarted = false;
        silenceFrames = 0;
        speechFrames = 0;
        frames = 0;
        noiseFloor = initialNoiseFloor;
    }

    Result process(byte[] bytes) {
        frames++;
        int level = averageAbsPcm16(bytes);
        int visualLevel = Math.min(100, level / 45);
        int adaptiveThreshold = Math.max(speechThreshold, (int) (noiseFloor * noiseMultiplier + noiseOffset));
        boolean speechCandidate = level > adaptiveThreshold;
        if (speechCandidate) {
            speechFrames++;
        } else {
            speechFrames = 0;
            noiseFloor = noiseFloor * 0.96f + level * 0.04f;
        }
        boolean speech = speechFrames >= MIN_SPEECH_FRAMES;
        if (speech) {
            speechStarted = true;
            silenceFrames = 0;
            return new Result(level, visualLevel, true, false);
        }
        if (speechStarted) {
            silenceFrames++;
        }
        boolean shouldEndInput = speechStarted
                && frames > MIN_FRAMES_BEFORE_END
                && silenceFrames >= SILENCE_FRAMES_TO_END;
        return new Result(level, visualLevel, false, shouldEndInput);
    }

    static int averageAbsPcm16(byte[] bytes) {
        if (bytes == null) return 0;
        return averageAbsPcm16(bytes, bytes.length);
    }

    static int averageAbsPcm16(byte[] bytes, int length) {
        if (bytes == null || length <= 0) return 0;
        int safeLength = Math.min(length, bytes.length);
        int samples = safeLength / 2;
        if (samples == 0) return 0;
        long total = 0;
        for (int i = 0; i + 1 < safeLength; i += 2) {
            int sample = (short) ((bytes[i] & 0xff) | (bytes[i + 1] << 8));
            total += Math.abs(sample);
        }
        return (int) (total / samples);
    }
}
