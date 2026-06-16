package com.linkyun.her;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

final class VoiceprintGate {
    static final int VERIFY_BYTES = 48_000;
    static final float ACCEPT_THRESHOLD = 0.64f;
    private final VoiceprintEngine engine;
    private final float[] enrolled;
    private final ByteArrayOutputStream bufferedAudio = new ByteArrayOutputStream();
    private final List<byte[]> bufferedFrames = new ArrayList<>();
    private boolean decided;
    private boolean accepted;
    private float score;

    VoiceprintGate(VoiceprintEngine engine, float[] enrolled) {
        this.engine = engine;
        this.enrolled = enrolled == null ? new float[0] : enrolled;
    }

    static VoiceprintGate passthrough(VoiceprintEngine engine) {
        VoiceprintGate gate = new VoiceprintGate(engine, new float[0]);
        gate.decided = true;
        gate.accepted = true;
        return gate;
    }

    boolean isDecided() {
        return decided;
    }

    Result accept(byte[] bytes) {
        if (decided) {
            List<byte[]> frames = new ArrayList<>();
            if (accepted && bytes != null && bytes.length > 0) frames.add(bytes);
            return new Result(accepted, !accepted, score, frames);
        }
        if (bytes != null && bytes.length > 0) {
            byte[] copy = new byte[bytes.length];
            System.arraycopy(bytes, 0, copy, 0, bytes.length);
            bufferedFrames.add(copy);
            bufferedAudio.write(copy, 0, copy.length);
        }
        if (bufferedAudio.size() < VERIFY_BYTES) {
            return Result.pending();
        }
        score = engine.score(enrolled, engine.embed(bufferedAudio.toByteArray()));
        decided = true;
        accepted = score >= ACCEPT_THRESHOLD;
        if (!accepted) return new Result(false, true, score, new ArrayList<>());
        List<byte[]> release = new ArrayList<>(bufferedFrames);
        bufferedFrames.clear();
        return new Result(true, false, score, release);
    }

    static final class Result {
        final boolean accepted;
        final boolean rejected;
        final float score;
        final List<byte[]> frames;

        private Result(boolean accepted, boolean rejected, float score, List<byte[]> frames) {
            this.accepted = accepted;
            this.rejected = rejected;
            this.score = score;
            this.frames = frames;
        }

        static Result pending() {
            return new Result(false, false, 0, new ArrayList<>());
        }
    }
}
