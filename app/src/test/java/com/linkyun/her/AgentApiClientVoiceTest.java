package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AgentApiClientVoiceTest {
    @Test
    public void realtimeVoiceFilterExcludesKnownUnsupportedEmoVoices() {
        assertFalse(AgentApiClient.isRealtimeSupportedVoiceId(
                "zh_female_roumeinvyou_emo_v2_mars_bigtts"));
        assertFalse(AgentApiClient.isRealtimeSupportedVoiceId(
                "zh_male_jingqiangkanye_emo_mars_bigtts"));
        assertFalse(AgentApiClient.isRealtimeSupportedVoiceId(
                "zh_male_zhoujielun_emo_v2_mars_bigtts"));
    }

    @Test
    public void realtimeVoiceFilterKeepsVerifiedVoicesAndJessClone() {
        assertTrue(AgentApiClient.isRealtimeSupportedVoiceId(
                "zh_female_vv_jupiter_bigtts"));
        assertTrue(AgentApiClient.isRealtimeSupportedVoiceId(
                "zh_female_xiaohe_jupiter_bigtts"));
        assertTrue(AgentApiClient.isRealtimeSupportedVoiceId(
                "zh_male_yunzhou_jupiter_bigtts"));
        assertTrue(AgentApiClient.isRealtimeSupportedVoiceId("S_VCQjam1U1"));
    }

    @Test
    public void realtimeVoiceFilterRejectsBlankIds() {
        assertFalse(AgentApiClient.isRealtimeSupportedVoiceId(null));
        assertFalse(AgentApiClient.isRealtimeSupportedVoiceId("   "));
    }
}
