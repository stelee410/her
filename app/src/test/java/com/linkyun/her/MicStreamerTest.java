package com.linkyun.her;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.media.AudioDeviceInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public final class MicStreamerTest {
    @Test
    public void constructorFailureReturnsFalseWithoutRunning() {
        FakeFactory factory = new FakeFactory();
        factory.throwOnCreate = true;
        MicStreamer streamer = new MicStreamer(factory, AudioFrameProcessor.NONE);

        assertFalse(streamer.start(bytes -> { }));

        assertFalse(streamer.running);
        assertEquals(0, factory.effectsEnabled);
        assertEquals(0, factory.effectsReleased);
    }

    @Test
    public void uninitializedRecorderIsReleasedAndDoesNotRun() {
        FakeFactory factory = new FakeFactory();
        factory.recorder.initialized = false;
        MicStreamer streamer = new MicStreamer(factory, AudioFrameProcessor.NONE);

        assertFalse(streamer.start(bytes -> { }));

        assertFalse(streamer.running);
        assertEquals(1, factory.recorder.releaseCount);
        assertEquals(0, factory.effectsEnabled);
    }

    @Test
    public void startRecordingFailureCleansRecorderAndEffects() {
        FakeFactory factory = new FakeFactory();
        factory.recorder.throwOnStart = true;
        MicStreamer streamer = new MicStreamer(factory, AudioFrameProcessor.NONE);

        assertFalse(streamer.start(bytes -> { }));

        assertFalse(streamer.running);
        assertEquals(1, factory.effectsEnabled);
        assertEquals(1, factory.effectsReleased);
        assertEquals(1, factory.recorder.releaseCount);
    }

    @Test
    public void startReadsAudioUntilStopped() throws Exception {
        FakeFactory factory = new FakeFactory();
        MicStreamer streamer = new MicStreamer(factory, AudioFrameProcessor.NONE);
        CountDownLatch audioRead = new CountDownLatch(1);
        byte[][] received = new byte[1][];

        assertTrue(streamer.start(bytes -> {
            received[0] = bytes;
            audioRead.countDown();
        }));
        assertTrue(audioRead.await(1, TimeUnit.SECONDS));
        streamer.stop();

        assertArrayEquals(new byte[]{1, 2}, received[0]);
        assertFalse(streamer.running);
        assertEquals(16000, factory.bufferSize);
        assertEquals(android.media.MediaRecorder.AudioSource.VOICE_COMMUNICATION, factory.audioSource);
        assertEquals(1, factory.effectsEnabled);
        assertEquals(1, factory.effectsReleased);
        assertEquals(1, factory.recorder.releaseCount);
    }

    @Test
    public void rawMicUsesVoiceRecognitionSourceWithoutEffectsOrProcessor() throws Exception {
        FakeFactory factory = new FakeFactory();
        AudioFrameProcessor processor = new AudioFrameProcessor() {
            @Override public byte[] process(byte[] buffer, int length) {
                return new byte[0];
            }

            @Override public void reset() {
            }
        };
        MicStreamer streamer = new MicStreamer(factory, processor);
        CountDownLatch audioRead = new CountDownLatch(1);
        byte[][] received = new byte[1][];

        assertTrue(streamer.startRawMic(null, bytes -> {
            received[0] = bytes;
            audioRead.countDown();
        }));
        assertTrue(audioRead.await(1, TimeUnit.SECONDS));
        streamer.stop();

        assertArrayEquals(new byte[]{1, 2}, received[0]);
        assertEquals(android.media.MediaRecorder.AudioSource.VOICE_RECOGNITION, factory.audioSource);
        assertEquals(0, factory.effectsEnabled);
        assertEquals(0, factory.effectsReleased);
    }

    @Test
    public void readFailureStopsAndReleasesRecorderWithoutUncaughtException() throws Exception {
        Thread.UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        AtomicReference<Throwable> uncaught = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> uncaught.set(error));
        try {
            FakeFactory factory = new FakeFactory();
            factory.recorder.throwOnRead = true;
            MicStreamer streamer = new MicStreamer(factory, AudioFrameProcessor.NONE);

            assertTrue(streamer.start(bytes -> { }));
            assertTrue(factory.recorder.readAttempt.await(1, TimeUnit.SECONDS));
            waitUntilStopped(streamer);
            streamer.stop();

            assertFalse(streamer.running);
            assertEquals(1, factory.effectsReleased);
            assertEquals(1, factory.recorder.releaseCount);
            assertEquals(null, uncaught.get());
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
    }

    private static void waitUntilStopped(MicStreamer streamer) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (streamer.running && System.nanoTime() < deadline) {
            Thread.sleep(2);
        }
    }

    private static final class FakeFactory implements MicStreamer.RecorderFactory {
        final FakeRecorder recorder = new FakeRecorder();
        boolean throwOnCreate;
        int bufferSize;
        int audioSource;
        int effectsEnabled;
        int effectsReleased;

        @Override public int minBufferSize() {
            return -2;
        }

        @Override public MicStreamer.Recorder create(int bufferSize, AudioDeviceInfo preferredDevice,
                int audioSource) {
            if (throwOnCreate) throw new IllegalArgumentException("bad recorder");
            this.bufferSize = bufferSize;
            this.audioSource = audioSource;
            return recorder;
        }

        @Override public void enableEffects(int sessionId) {
            effectsEnabled++;
        }

        @Override public void releaseEffects() {
            effectsReleased++;
        }
    }

    private static final class FakeRecorder implements MicStreamer.Recorder {
        boolean initialized = true;
        boolean throwOnStart;
        boolean throwOnRead;
        final CountDownLatch readAttempt = new CountDownLatch(1);
        int reads;
        int releaseCount;

        @Override public boolean isInitialized() {
            return initialized;
        }

        @Override public int audioSessionId() {
            return 7;
        }

        @Override public void startRecording() {
            if (throwOnStart) throw new IllegalStateException("cannot start");
        }

        @Override public int read(byte[] buffer, int offset, int length) {
            readAttempt.countDown();
            if (throwOnRead) throw new IllegalStateException("read failed");
            reads++;
            if (reads == 1) {
                buffer[offset] = 1;
                buffer[offset + 1] = 2;
                return 2;
            }
            try {
                Thread.sleep(2);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            return 0;
        }

        @Override public void stop() {
        }

        @Override public void release() {
            releaseCount++;
        }
    }
}
