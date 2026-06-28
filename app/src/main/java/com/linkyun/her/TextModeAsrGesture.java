package com.linkyun.her;

final class TextModeAsrGesture {
    static final int NEUTRAL = 0;
    static final int CANCEL = 1;
    static final int SEND = 2;

    private TextModeAsrGesture() { }

    static int decide(float deltaY, float thresholdPx) {
        if (deltaY <= -thresholdPx) return CANCEL;
        return NEUTRAL;
    }

    static String label(int state) {
        if (state == CANCEL) return "松手取消";
        if (state == SEND) return "松开发送";
        return "松开发送    上滑取消";
    }
}
