package com.linkyun.her;

import java.io.ByteArrayOutputStream;

final class VoiceprintEnrollmentCapture {
    static final int TARGET_BYTES = 96_000;
    private final VoiceprintEngine engine;
    private final ByteArrayOutputStream audio = new ByteArrayOutputStream();
    private int voicedFrames;

    VoiceprintEnrollmentCapture(VoiceprintEngine engine) {
        this.engine = engine;
    }

    Result accept(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return Result.pending(progress());
        audio.write(bytes, 0, bytes.length);
        if (VoiceActivityDetector.averageAbsPcm16(bytes) > 420) voicedFrames++;
        if (audio.size() < TARGET_BYTES) return Result.pending(progress());
        if (voicedFrames < 25) return Result.failed("声纹录入失败：说话声音太少，请靠近麦克风重试。");
        return Result.complete(engine.embed(audio.toByteArray()), progress());
    }

    int progress() {
        return Math.min(100, (audio.size() * 100) / TARGET_BYTES);
    }

    static final class Result {
        final boolean complete;
        final boolean failed;
        final String message;
        final float[] embedding;
        final int progress;

        private Result(boolean complete, boolean failed, String message, float[] embedding, int progress) {
            this.complete = complete;
            this.failed = failed;
            this.message = message;
            this.embedding = embedding;
            this.progress = progress;
        }

        static Result pending(int progress) {
            return new Result(false, false, "", null, progress);
        }

        static Result complete(float[] embedding, int progress) {
            return new Result(true, false, "", embedding, progress);
        }

        static Result failed(String message) {
            return new Result(false, true, message, null, 100);
        }
    }
}
