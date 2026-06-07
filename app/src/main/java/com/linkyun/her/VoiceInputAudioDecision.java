package com.linkyun.her;

final class VoiceInputAudioDecision {
    private VoiceInputAudioDecision() { }

    static boolean canStart(boolean textModeActive,
            boolean voiceSurfaceActive,
            boolean textOnlyState,
            boolean micRunning,
            boolean inputAudioOpen,
            boolean hasRecordPermission) {
        return !textModeActive && voiceSurfaceActive && !textOnlyState && !micRunning &&
                !inputAudioOpen && hasRecordPermission;
    }

    static StopDecision stop(boolean inputAudioOpen, String nextState) {
        boolean sendInputEnd = inputAudioOpen;
        boolean scheduleAsrFinalTimeout = sendInputEnd &&
                VoiceSessionState.fromLegacy(nextState).isProcessing();
        return new StopDecision(sendInputEnd, scheduleAsrFinalTimeout);
    }

    static final class StopDecision {
        final boolean sendInputEnd;
        final boolean scheduleAsrFinalTimeout;

        StopDecision(boolean sendInputEnd, boolean scheduleAsrFinalTimeout) {
            this.sendInputEnd = sendInputEnd;
            this.scheduleAsrFinalTimeout = scheduleAsrFinalTimeout;
        }
    }
}
