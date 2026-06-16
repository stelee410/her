package com.linkyun.her;

final class TvVoiceCommand {
    private TvVoiceCommand() {
    }

    static boolean shouldOpen(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) return false;
        return normalized.contains("我要看电视")
                || normalized.contains("我想看电视")
                || normalized.contains("打开电视")
                || normalized.contains("看电视")
                || normalized.contains("电视模式")
                || normalized.contains("playtv")
                || normalized.contains("watchtv");
    }

    private static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replace("，", "")
                .replace("。", "")
                .replace("！", "")
                .replace("？", "")
                .replace(",", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .replace(" ", "")
                .trim();
    }
}
