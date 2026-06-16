package com.linkyun.her;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class NoiseSuppressingAudioProcessorTest {
    @Test
    public void quietFramesAreAttenuatedMoreThanSpeechFrames() {
        NoiseSuppressingAudioProcessor processor = new NoiseSuppressingAudioProcessor();

        byte[] quiet = processor.process(tone(80, 32, 160), 320);
        byte[] speech = processor.process(tone(2600, 12, 160), 320);

        assertTrue(VoiceActivityDetector.averageAbsPcm16(quiet) < 80);
        assertTrue(VoiceActivityDetector.averageAbsPcm16(speech) > 800);
    }

    private static byte[] tone(int amplitude, int periodSamples, int count) {
        byte[] bytes = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            short value = (short) (Math.sin(i * 2.0 * Math.PI / periodSamples) * amplitude);
            bytes[i * 2] = (byte) (value & 0xff);
            bytes[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
        }
        return bytes;
    }
}
