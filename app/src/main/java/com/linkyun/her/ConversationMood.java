package com.linkyun.her;

import java.util.Locale;

final class ConversationMood {
    private ConversationMood() {
    }

    static int forText(String text) {
        String value = text == null ? "" : text.toLowerCase(Locale.US);
        if (value.contains("焦虑") || value.contains("难受") || value.contains("害怕") ||
                value.contains("崩溃") || value.contains("anxious") || value.contains("sad")) {
            return 1;
        }
        if (value.contains("开心") || value.contains("高兴") || value.contains("喜欢") ||
                value.contains("期待") || value.contains("happy") || value.contains("love")) {
            return 2;
        }
        if (value.contains("累") || value.contains("困") || value.contains("疲惫") ||
                value.contains("安静") || value.contains("tired") || value.contains("quiet")) {
            return 3;
        }
        return 0;
    }
}
