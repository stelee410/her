package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GatewayTtsPlayerTextTest {
    @Test
    public void sanitizeForSpeechRemovesBracketedAsides() {
        assertEquals("你好，今天开始吧。",
                GatewayTtsPlayer.sanitizeForSpeech("（轻轻笑）你好，[别读这个]今天开始吧。"));
    }

    @Test
    public void sanitizeForSpeechRemovesFormattedSpans() {
        assertEquals("我在这里。继续说。",
                GatewayTtsPlayer.sanitizeForSpeech("我在这里。**动作：靠近一点** `stage note` 继续说。"));
    }

    @Test
    public void sanitizeForSpeechStripsLineFormattingMarkers() {
        assertEquals("第一句 第二句",
                GatewayTtsPlayer.sanitizeForSpeech("# 第一句\n- 第二句\n---"));
    }

    @Test
    public void sanitizeForSpeechCanReturnEmptyWhenOnlyNonSpokenContentRemains() {
        assertEquals("",
                GatewayTtsPlayer.sanitizeForSpeech("（沉默片刻） **不要朗读** `meta`"));
    }
}
