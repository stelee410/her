package com.linkyun.her;

final class TextModeAsrGesture {
    static final int NEUTRAL = 0;
    static final int CANCEL = 1;
    static final int SEND = 2;

    private TextModeAsrGesture() { }

    static int decide(float deltaX, float thresholdPx) {
        if (deltaX <= -thresholdPx) return CANCEL;
        if (deltaX >= thresholdPx) return SEND;
        return NEUTRAL;
    }

    static String label(int state) {
        if (state == CANCEL) return "← 取消    ▌ ▌ ▌";
        if (state == SEND) return "▌ ▌ ▌    发送 →";
        return "← 取消    ▌ ▌ ▌    发送 →";
    }
}
