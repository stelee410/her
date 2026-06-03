package com.linkyun.her;

final class VoicePipelineState {
    enum Display {
        IDLE,
        CONNECTING,
        READY,
        LISTENING,
        PROCESSING,
        SPEAKING_REALTIME,
        SPEAKING_TOOL_TTS,
        NEWS_ACK,
        TOOL_RUNNING,
        SUMMARIZING,
        TEXT_ONLY,
        ERROR
    }

    enum ToolTts {
        IDLE,
        QUEUED,
        WAITING_REALTIME_STOP,
        REQUESTING,
        PLAYING
    }

    enum RealtimeOutput {
        IDLE,
        STREAMING,
        DISCARDING_UNTIL_DONE
    }

    private VoicePipelineState() {
    }
}
