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
                || shouldOpenFinanceChannel(normalized)
                || normalized.contains("playtv")
                || normalized.contains("watchtv");
    }

    static String preferredChannelId(String text) {
        return shouldOpenFinanceChannel(normalize(text)) ? OnlineTvPlaylist.DEFAULT_FINANCE_CHANNEL_ID : "";
    }

    private static boolean shouldOpenFinanceChannel(String normalized) {
        if (normalized.isEmpty()) return false;
        if (normalized.contains("财经新闻")
                || normalized.contains("财经频道")
                || normalized.contains("财经直播")
                || normalized.contains("财经节目")
                || normalized.contains("商业新闻")
                || normalized.contains("金融新闻")
                || normalized.contains("市场资讯")
                || normalized.contains("股市新闻")
                || normalized.contains("股票新闻")) {
            return true;
        }
        return normalized.contains("财经") &&
                (normalized.contains("看") ||
                        normalized.contains("了解") ||
                        normalized.contains("打开") ||
                        normalized.contains("播放") ||
                        normalized.contains("频道") ||
                        normalized.contains("新闻"));
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
