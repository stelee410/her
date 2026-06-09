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

    PcmPlayer(String tag) {
        this.tag = tag;
    }

    synchronized void begin(int rate) {
        begin(rate, false);
    }

    synchronized void begin(int rate, boolean useVoiceCommunication) {
        sampleRate = rate;
        if (voiceCommunication != useVoiceCommunication) {
            stop();
        }
        voiceCommunication = useVoiceCommunication;
        Log.d(tag, "player begin sampleRate=" + sampleRate);
        ensureTrack();
    }

    synchronized void play(byte[] bytes) {
        ensureTrack();
        int written = track.write(bytes, 0, bytes.length);
        Log.d(tag, "player write bytes=" + bytes.length + " written=" + written + " state=" + track.getPlayState());
    }

    synchronized void stop() {
        if (track != null) {
            try { track.pause(); } catch (Exception ignored) { }
            try { track.flush(); } catch (Exception ignored) { }
            track.release();
            track = null;
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
        Log.d(tag, "AudioTrack playState=" + track.getPlayState());
    }
}
