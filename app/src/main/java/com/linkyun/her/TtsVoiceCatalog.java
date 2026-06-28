package com.linkyun.her;

import java.util.ArrayList;
import java.util.List;

final class TtsVoiceCatalog {
    static final String DEFAULT_ID = "zh_female_vv_uranus_bigtts";

    private static final Voice[] USABLE = {
            new Voice("zh_female_vv_uranus_bigtts", "vivi 2.0", "female", "seed2"),
            new Voice("saturn_zh_female_cancan_tob", "知性灿灿", "female", "seed2"),
            new Voice("saturn_zh_female_keainvsheng_tob", "可爱女生", "female", "seed2"),
            new Voice("saturn_zh_female_tiaopigongzhu_tob", "调皮公主", "female", "seed2"),
            new Voice("saturn_zh_male_shuanglangshaonian_tob", "爽朗少年", "male", "seed2"),
            new Voice("saturn_zh_male_tiancaitongzhuo_tob", "天才同桌", "male", "seed2"),
            new Voice("zh_female_cancan_mars_bigtts", "灿灿 (mars)", "female", "bigtts"),
            new Voice("zh_female_zhixingnvsheng_mars_bigtts", "知性女声", "female", "bigtts"),
            new Voice("zh_female_qingxinnvsheng_mars_bigtts", "清新女声", "female", "bigtts"),
            new Voice("S_VCQjam1U1", "杰西卡（克隆音色）", "female", "v1")
    };

    private TtsVoiceCatalog() {
    }

    static List<Voice> usableVoices() {
        List<Voice> voices = new ArrayList<>();
        for (Voice voice : USABLE) {
            voices.add(voice);
        }
        return voices;
    }

    static Voice find(String id) {
        for (Voice voice : USABLE) {
            if (voice.id.equals(id)) return voice;
        }
        return USABLE[0];
    }

    static String labelFor(String id) {
        return find(id).label;
    }

    static String[] playbackOrder(String selectedId) {
        List<String> ids = new ArrayList<>();
        addIfMissing(ids, DEFAULT_ID);
        for (Voice voice : USABLE) {
            addIfMissing(ids, voice.id);
        }
        ids.remove(find(selectedId).id);
        return ids.toArray(new String[0]);
    }

    static String[] voiceOrder(String selectedId) {
        List<String> ids = new ArrayList<>();
        addIfMissing(ids, find(selectedId).id);
        addIfMissing(ids, DEFAULT_ID);
        for (Voice voice : USABLE) {
            addIfMissing(ids, voice.id);
        }
        return ids.toArray(new String[0]);
    }

    private static void addIfMissing(List<String> ids, String id) {
        if (id == null || id.trim().isEmpty()) return;
        for (String existing : ids) {
            if (existing.equals(id)) return;
        }
        ids.add(id);
    }
}
