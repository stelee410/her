package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ErrorDisplayDecisionTest {
    @Test
    public void updatesInitializationLastTurnWhenVisible() {
        ErrorDisplayDecision decision = ErrorDisplayDecision.decide(
                "失败了", true, false, false);

        assertEquals("失败了", decision.message);
        assertTrue(decision.updateInitializationLastTurn);
        assertFalse(decision.appendAssistantMessage);
        assertFalse(decision.renderMessages);
        assertTrue(decision.updateVoiceHome);
    }

    @Test
    public void appendsMessageWhenChatOrVoiceSurfaceCanShowIt() {
        ErrorDisplayDecision chat = ErrorDisplayDecision.decide("错误", false, true, false);
        ErrorDisplayDecision voice = ErrorDisplayDecision.decide("错误", false, false, true);

        assertTrue(chat.appendAssistantMessage);
        assertTrue(chat.renderMessages);
        assertTrue(voice.appendAssistantMessage);
        assertFalse(voice.renderMessages);
    }

    @Test
    public void nullMessageBecomesEmptyString() {
        ErrorDisplayDecision decision = ErrorDisplayDecision.decide(null, false, false, false);

        assertEquals("", decision.message);
        assertFalse(decision.appendAssistantMessage);
        assertFalse(decision.renderMessages);
    }
}
