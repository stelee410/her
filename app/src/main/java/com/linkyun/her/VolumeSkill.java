package com.linkyun.her;

final class VolumeSkill {
    enum Direction {
        UP,
        DOWN
    }

    private VolumeSkill() { }

    static Direction direction(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return null;
        if (!mentionsVolume(normalized)) return null;
        if (containsAny(normalized, "大一点", "大点", "调大", "调高", "高一点", "高点",
                "响一点", "响点", "增加", "加大", "放大", "升高")) {
            return Direction.UP;
        }
        if (containsAny(normalized, "小一点", "小点", "调小", "调低", "低一点", "低点",
                "轻一点", "轻点", "减小", "降低", "降一点", "太大", "太响")) {
            return Direction.DOWN;
        }
        return null;
    }

    static boolean isVolumeCommand(String text) {
        return direction(text) != null;
    }

    private static boolean mentionsVolume(String text) {
        return containsAny(text, "声音", "音量", "声量", "播放", "你说话");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }

    private static String normalize(String text) {
        return text == null ? "" : text.trim().replaceAll("\\s+", "");
    }
}
