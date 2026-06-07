package com.linkyun.her;

final class RealtimeEventType {
    enum Kind {
        SESSION_CREATED,
        ASR_FINAL,
        ASSISTANT_STATE,
        ASSISTANT_TEXT_DELTA,
        OUTPUT_AUDIO_START,
        OUTPUT_AUDIO_DONE,
        OUTPUT_AUDIO_STOP,
        MEMORY_SNAPSHOT,
        ERROR,
        IGNORE
    }

    private RealtimeEventType() {
    }

    static Kind classify(String type, boolean hasPayload) {
        if ("session.created".equals(type)) return Kind.SESSION_CREATED;
        if ("output_audio.done".equals(type)) return Kind.OUTPUT_AUDIO_DONE;
        if ("output_audio.stop".equals(type)) return Kind.OUTPUT_AUDIO_STOP;
        if (!hasPayload) return Kind.IGNORE;
        if ("asr.final".equals(type)) return Kind.ASR_FINAL;
        if ("assistant.state".equals(type)) return Kind.ASSISTANT_STATE;
        if ("assistant.text.delta".equals(type)) return Kind.ASSISTANT_TEXT_DELTA;
        if ("output_audio.start".equals(type)) return Kind.OUTPUT_AUDIO_START;
        if ("memory.snapshot".equals(type)) return Kind.MEMORY_SNAPSHOT;
        if ("error".equals(type)) return Kind.ERROR;
        return Kind.IGNORE;
    }
}
