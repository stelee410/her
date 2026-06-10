package com.linkyun.her;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

final class PcmPlayer {
    private final String tag;
    private AudioTrack track;
    private int sampleRate = 24000;
    private boolean voiceCommunication;
    private long framesWritten;
    private float volumeGain = 1.0f;

    PcmPlayer(String tag) {
        this.tag = tag;
    }

    synchronized void begin(int rate) {
        begin(rate, false);
    }

    synchronized void begin(int rate, boolean useVoiceCommunication) {
        stop();
        sampleRate = rate;
        voiceCommunication = useVoiceCommunication;
        framesWritten = 0;
        Log.d(tag, "player begin sampleRate=" + sampleRate);
        ensureTrack();
    }

    synchronized void play(byte[] bytes) {
        ensureTrack();
        track.setVolume(volumeGain);
        int written = track.write(bytes, 0, bytes.length);
        if (written > 0) framesWritten += written / 2;
        Log.d(tag, "player write bytes=" + bytes.length + " written=" + written + " state=" + track.getPlayState());
    }

    synchronized void setVolumeGain(float gain) {
        volumeGain = clampGain(gain);
        if (track != null) track.setVolume(volumeGain);
    }

    synchronized long playbackDrainDelayMs() {
        if (track == null || sampleRate <= 0) return 0;
        try {
            long played = track.getPlaybackHeadPosition() & 0xffffffffL;
            long pendingFrames = Math.max(0, framesWritten - played);
            long pendingMs = (pendingFrames * 1000L) / sampleRate;
            return Math.max(500, Math.min(1600, pendingMs + 260));
        } catch (RuntimeException ignored) {
            return 650;
        }
    }

    synchronized void stop() {
        if (track != null) {
            try { track.pause(); } catch (Exception ignored) { }
            try { track.flush(); } catch (Exception ignored) { }
            track.release();
            track = null;
            framesWritten = 0;
        }
    }

    private void ensureTrack() {
        if (track != null) return;
        int min = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        Log.d(tag, "create AudioTrack min=" + min + " rate=" + sampleRate);
        track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(voiceCommunication
                                ? AudioAttributes.USAGE_VOICE_COMMUNICATION
                                : AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(Math.max(min, sampleRate))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        track.play();
        track.setVolume(volumeGain);
        Log.d(tag, "AudioTrack playState=" + track.getPlayState());
    }

    private static float clampGain(float gain) {
        if (Float.isNaN(gain)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, gain));
    }
}
