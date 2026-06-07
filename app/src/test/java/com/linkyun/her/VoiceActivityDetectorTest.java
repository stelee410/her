package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceActivityDetectorTest {
    @Test
    public void averageAbsPcm16ReadsLittleEndianSignedSamples() {
        assertEquals(750, VoiceActivityDetector.averageAbsPcm16(pcm((short) 1000, (short) -500)));
        assertEquals(0, VoiceActivityDetector.averageAbsPcm16(new byte[] {1}));
        assertEquals(0, VoiceActivityDetector.averageAbsPcm16(null));
    }

    @Test
    public void visualLevelIsCappedAtOneHundred() {
        VoiceActivityDetector detector = new VoiceActivityDetector();

        VoiceActivityDetector.Result result = detector.process(pcm((short) 9000));

        assertEquals(9000, result.level);
        assertEquals(100, result.visualLevel);
        assertFalse(result.shouldEndInput);
    }

    @Test
    public void silenceBeforeSpeechNeverEndsInput() {
        VoiceActivityDetector detector = new VoiceActivityDetector();
        VoiceActivityDetector.Result result = null;

        for (int i = 0; i < 80; i++) {
            result = detector.process(pcm((short) 0));
        }

        assertFalse(result.shouldEndInput);
    }

    @Test
    public void endsInputAfterSpeechAndEnoughSilence() {
        VoiceActivityDetector detector = new VoiceActivityDetector();
        assertFalse(detector.process(pcm((short) 900)).shouldEndInput);

        VoiceActivityDetector.Result result = null;
        for (int i = 0; i < 44; i++) {
            result = detector.process(pcm((short) 0));
            assertFalse(result.shouldEndInput);
        }

        result = detector.process(pcm((short) 0));
        assertTrue(result.shouldEndInput);
    }

    @Test
    public void resetRequiresFreshSpeechBeforeEndingAgain() {
        VoiceActivityDetector detector = new VoiceActivityDetector();
        detector.process(pcm((short) 900));
        detector.reset();

        VoiceActivityDetector.Result result = null;
        for (int i = 0; i < 60; i++) {
            result = detector.process(pcm((short) 0));
        }

        assertFalse(result.shouldEndInput);
    }

    private static byte[] pcm(short... samples) {
        byte[] bytes = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            bytes[i * 2] = (byte) (samples[i] & 0xff);
            bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xff);
        }
        return bytes;
    }
}
