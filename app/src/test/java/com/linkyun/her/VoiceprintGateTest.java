package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceprintGateTest {
    @Test
    public void passthroughReturnsCurrentFrameImmediately() {
        VoiceprintGate gate = VoiceprintGate.passthrough(new SignalVoiceprintEngine());

        VoiceprintGate.Result result = gate.accept(pcm(440, 320));

        assertTrue(result.accepted);
        assertFalse(result.frames.isEmpty());
    }

    @Test
    public void matchingVoiceprintReleasesBufferedFrames() {
        SignalVoiceprintEngine engine = new SignalVoiceprintEngine();
        byte[] audio = pcm(440, VoiceprintGate.VERIFY_BYTES / 2);
        VoiceprintGate gate = new VoiceprintGate(engine, engine.embed(audio));

        VoiceprintGate.Result result = gate.accept(audio);

        assertTrue(result.accepted);
        assertFalse(result.frames.isEmpty());
    }

    @Test
    public void differentVoiceprintRejectsBufferedFrames() {
        SignalVoiceprintEngine engine = new SignalVoiceprintEngine();
        VoiceprintGate gate = new VoiceprintGate(engine,
                engine.embed(pcm(220, VoiceprintGate.VERIFY_BYTES / 2)));

        VoiceprintGate.Result result = gate.accept(pcm(1800, VoiceprintGate.VERIFY_BYTES / 2));

        assertTrue(result.rejected);
        assertTrue(result.frames.isEmpty());
    }

    private static byte[] pcm(int periodSamples, int samples) {
        byte[] bytes = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            short sample = (short) (Math.sin(i * 2.0 * Math.PI / periodSamples) * 2800);
            bytes[i * 2] = (byte) (sample & 0xff);
            bytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        return bytes;
    }
}
