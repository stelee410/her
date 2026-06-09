package com.linkyun.her;

import android.annotation.SuppressLint;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

final class MicStreamer {
    interface Recorder {
        boolean isInitialized();
        int audioSessionId();
        void startRecording();
        int read(byte[] buffer, int offset, int length);
        void stop();
        void release();
    }

    interface RecorderFactory {
        int minBufferSize();
        Recorder create(int bufferSize, AudioDeviceInfo preferredDevice);
        void enableEffects(int sessionId);
        void releaseEffects();
    }

    volatile boolean running = false;
    private final RecorderFactory recorderFactory;
    private Recorder recorder;
    private Thread thread;
    private boolean effectsActive;

    MicStreamer() {
        this(new AndroidRecorderFactory());
    }

    MicStreamer(RecorderFactory recorderFactory) {
        this.recorderFactory = recorderFactory;
    }

    boolean start(AudioSink sink) {
        return start(null, sink);
    }

    boolean start(AudioDeviceInfo preferredDevice, AudioSink sink) {
        if (running) return true;
        try {
            int min = recorderFactory.minBufferSize();
            int bufferSize = Math.max(min, 16000);
            recorder = recorderFactory.create(bufferSize, preferredDevice);
        } catch (IllegalArgumentException | SecurityException error) {
            recorder = null;
            return false;
        }
        if (!recorder.isInitialized()) {
            recorder.release();
            recorder = null;
            return false;
        }
        recorderFactory.enableEffects(recorder.audioSessionId());
        effectsActive = true;
        running = true;
        try {
            recorder.startRecording();
        } catch (IllegalArgumentException | IllegalStateException | SecurityException error) {
            running = false;
            releaseRecorder();
            return false;
        }
        Recorder activeRecorder = recorder;
        thread = new Thread(() -> {
            byte[] buffer = new byte[640];
            while (running) {
                int read;
                try {
                    read = activeRecorder.read(buffer, 0, buffer.length);
                } catch (RuntimeException error) {
                    running = false;
                    releaseRecorder();
                    break;
                }
                if (read > 0) {
                    byte[] out = new byte[read];
                    System.arraycopy(buffer, 0, out, 0, read);
                    sink.onAudio(out);
                }
            }
        }, "her-mic");
        thread.start();
        return true;
    }

    void stop() {
        running = false;
        releaseRecorder();
    }

    private synchronized void releaseRecorder() {
        if (effectsActive) {
            recorderFactory.releaseEffects();
            effectsActive = false;
        }
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) { }
            recorder.release();
            recorder = null;
        }
    }

    private static final class AndroidRecorderFactory implements RecorderFactory {
        private AcousticEchoCanceler echoCanceler;
        private NoiseSuppressor noiseSuppressor;
        private AutomaticGainControl gainControl;

        @Override public int minBufferSize() {
            return AudioRecord.getMinBufferSize(16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
        }

        @SuppressLint("MissingPermission")
        @Override public Recorder create(int bufferSize, AudioDeviceInfo preferredDevice) {
            AudioRecord audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);
            if (preferredDevice != null) {
                try {
                    audioRecord.setPreferredDevice(preferredDevice);
                } catch (RuntimeException ignored) { }
            }
            return new AndroidRecorder(audioRecord);
        }

        @Override public void enableEffects(int sessionId) {
            try {
                if (AcousticEchoCanceler.isAvailable()) {
                    echoCanceler = AcousticEchoCanceler.create(sessionId);
                    if (echoCanceler != null) echoCanceler.setEnabled(true);
                }
            } catch (Throwable ignored) { }
            try {
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(sessionId);
                    if (noiseSuppressor != null) noiseSuppressor.setEnabled(true);
                }
            } catch (Throwable ignored) { }
            try {
                if (AutomaticGainControl.isAvailable()) {
                    gainControl = AutomaticGainControl.create(sessionId);
                    if (gainControl != null) gainControl.setEnabled(true);
                }
            } catch (Throwable ignored) { }
        }

        @Override public void releaseEffects() {
            if (echoCanceler != null) {
                echoCanceler.release();
                echoCanceler = null;
            }
            if (noiseSuppressor != null) {
                noiseSuppressor.release();
                noiseSuppressor = null;
            }
            if (gainControl != null) {
                gainControl.release();
                gainControl = null;
            }
        }
    }

    private static final class AndroidRecorder implements Recorder {
        private final AudioRecord audioRecord;

        AndroidRecorder(AudioRecord audioRecord) {
            this.audioRecord = audioRecord;
        }

        @Override public boolean isInitialized() {
            return audioRecord.getState() == AudioRecord.STATE_INITIALIZED;
        }

        @Override public int audioSessionId() {
            return audioRecord.getAudioSessionId();
        }

        @Override public void startRecording() {
            audioRecord.startRecording();
        }

        @Override public int read(byte[] buffer, int offset, int length) {
            return audioRecord.read(buffer, offset, length);
        }

        @Override public void stop() {
            audioRecord.stop();
        }

        @Override public void release() {
            audioRecord.release();
        }
    }
}
