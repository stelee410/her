package com.linkyun.her;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;

final class MicStreamer {
    volatile boolean running = false;
    private AudioRecord recorder;
    private AcousticEchoCanceler echoCanceler;
    private NoiseSuppressor noiseSuppressor;
    private AutomaticGainControl gainControl;
    private Thread thread;

    boolean start(AudioSink sink) {
        if (running) return true;
        int min = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(min, 16000);
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            recorder.release();
            recorder = null;
            return false;
        }
        enableEffects(recorder.getAudioSessionId());
        running = true;
        try {
            recorder.startRecording();
        } catch (IllegalStateException error) {
            running = false;
            releaseRecorder();
            return false;
        }
        thread = new Thread(() -> {
            byte[] buffer = new byte[640];
            while (running) {
                int read = recorder.read(buffer, 0, buffer.length);
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

    private void enableEffects(int sessionId) {
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

    private void releaseRecorder() {
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
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) { }
            recorder.release();
            recorder = null;
        }
    }
}
