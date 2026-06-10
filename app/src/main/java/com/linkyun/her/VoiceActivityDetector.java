package com.linkyun.her;

final class VoiceActivityDetector {
    private static final int SPEECH_THRESHOLD = 520;
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
    private int frames;

    void reset() {
        speechStarted = false;
        silenceFrames = 0;
        frames = 0;
    }

    Result process(byte[] bytes) {
        frames++;
        int level = averageAbsPcm16(bytes);
        int visualLevel = Math.min(100, level / 45);
        boolean speech = level > SPEECH_THRESHOLD;
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
        int samples = bytes.length / 2;
        if (samples == 0) return 0;
        long total = 0;
        for (int i = 0; i + 1 < bytes.length; i += 2) {
            int sample = (short) ((bytes[i] & 0xff) | (bytes[i + 1] << 8));
            total += Math.abs(sample);
        }
        return (int) (total / samples);
    }
}
