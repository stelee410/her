package com.linkyun.her;

import java.util.Locale;

final class AvatarVideoCatalog {
    static final String EMOTION_NEUTRAL = "neutral";
    static final String EMOTION_HAPPY = "happy";
    static final String EMOTION_UNHAPPY = "unhappy";
    static final String EMOTION_PLAYFUL = "playful";
    static final String EMOTION_SPORTS = "sports";

    private static final int[] WARDROBE_IDLE = {
            R.drawable.jess_stay_alpha,
            R.drawable.jess_sunglass_alpha,
            R.drawable.jess_swimming_suit_alpha,
            R.drawable.jess_with_cat_alpha,
            R.drawable.jess_play_tennis_alpha
    };
    private static final int[] IMAGE_CHANGE_ONCE = {
            R.drawable.jess_swimming_suit_alpha,
            R.drawable.jess_sunglass_alpha,
            R.drawable.jess_play_tennis_alpha,
            R.drawable.jess_sports_speak_alpha
    };

    private AvatarVideoCatalog() {
    }

    static String normalizeEmotion(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.US);
        if (EMOTION_HAPPY.equals(normalized) ||
                EMOTION_UNHAPPY.equals(normalized) ||
                EMOTION_PLAYFUL.equals(normalized) ||
                EMOTION_SPORTS.equals(normalized)) {
            return normalized;
        }
        return EMOTION_NEUTRAL;
    }

    static String emotionFromText(String text) {
        String value = text == null ? "" : text.toLowerCase(Locale.US);
        if (value.contains("运动") || value.contains("网球") || value.contains("跑步") ||
                value.contains("sports") || value.contains("tennis")) {
            return EMOTION_SPORTS;
        }
        if (value.contains("开心") || value.contains("高兴") || value.contains("喜欢") ||
                value.contains("期待") || value.contains("happy") || value.contains("love")) {
            return EMOTION_HAPPY;
        }
        if (value.contains("难受") || value.contains("沮丧") || value.contains("伤心") ||
                value.contains("焦虑") || value.contains("sad") || value.contains("anxious")) {
            return EMOTION_UNHAPPY;
        }
        if (value.contains("玩") || value.contains("调皮") || value.contains("轻松") ||
                value.contains("play") || value.contains("fun")) {
            return EMOTION_PLAYFUL;
        }
        return EMOTION_NEUTRAL;
    }

    static int idleVideo(String emotion, int wardrobeIndex) {
        int outfit = normalizedWardrobeIndex(wardrobeIndex);
        if (outfit != 0) return WARDROBE_IDLE[outfit];
        String normalized = normalizeEmotion(emotion);
        if (EMOTION_HAPPY.equals(normalized)) return R.drawable.jess_happy_alpha;
        if (EMOTION_UNHAPPY.equals(normalized)) return R.drawable.jess_unhappy_alpha;
        if (EMOTION_PLAYFUL.equals(normalized)) return R.drawable.jess_shake_alpha;
        if (EMOTION_SPORTS.equals(normalized)) return R.drawable.jess_play_tennis_alpha;
        return R.drawable.jess_stay_alpha;
    }

    static int speakingVideo(String emotion, int wardrobeIndex) {
        int outfit = normalizedWardrobeIndex(wardrobeIndex);
        String normalized = normalizeEmotion(emotion);
        if (outfit == 4 || EMOTION_SPORTS.equals(normalized)) return R.drawable.jess_sports_speak_alpha;
        if (outfit == 1) return R.drawable.jess_speak1_alpha;
        if (outfit == 2 || outfit == 3) return R.drawable.jess_speak2_alpha;
        return R.drawable.jess_speak_loop_alpha;
    }

    static int nextWardrobeIndex(int current) {
        return (normalizedWardrobeIndex(current) + 1) % WARDROBE_IDLE.length;
    }

    static int randomImageChangeVideo(int index) {
        if (index < 0) index = -index;
        return IMAGE_CHANGE_ONCE[index % IMAGE_CHANGE_ONCE.length];
    }

    static int petVideo() {
        return R.drawable.jess_with_cat_alpha;
    }

    private static int normalizedWardrobeIndex(int index) {
        if (index < 0) return 0;
        return index % WARDROBE_IDLE.length;
    }
}
