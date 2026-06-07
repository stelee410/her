package com.linkyun.her;

final class RealtimeAsrFinalText {
    static final class Result {
        final String text;
        final boolean hidden;
        final boolean resetIgnoreNextInitTrigger;

        Result(String text, boolean hidden, boolean resetIgnoreNextInitTrigger) {
            this.text = text;
            this.hidden = hidden;
            this.resetIgnoreNextInitTrigger = resetIgnoreNextInitTrigger;
        }
    }

    private RealtimeAsrFinalText() { }

    static Result classify(String rawText, boolean initializing) {
        String text = rawText == null ? "" : rawText.trim();
        if (isHiddenSystemEvent(text)) {
            return new Result(text, true, true);
        }
        if (initializing && isHiddenInitTrigger(text)) {
            return new Result(text, true, true);
        }
        return new Result(text, false, false);
    }

    static boolean isHiddenSystemEvent(String text) {
        if (text == null) return false;
        String value = text.trim();
        return value.startsWith("【系统事件】") || value.startsWith("[系统事件]");
    }

    static boolean isHiddenInitTrigger(String text) {
        return text != null && (text.contains("系统事件") ||
                text.contains("主动问候") ||
                text.contains("Agent 主动"));
    }
}
