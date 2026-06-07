package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ToolSessionTest {
    @Test
    public void startedSessionExposesQuestionRealtimeModeAndToken() {
        ToolSession session = ToolSession.start("news", "查新闻", true, 7);

        assertEquals("查新闻", session.question());
        assertTrue(session.realtimeMode());
        assertEquals(7, session.token());
        assertEquals("news", session.toolId());
        assertTrue(session.isRealtimeTool("news"));
        assertTrue(session.matches("news", 7));
    }

    @Test
    public void toolAndTokenMustBothMatch() {
        ToolSession session = ToolSession.start("weather", "深圳天气", false, 3);

        assertFalse(session.isRealtimeTool("weather"));
        assertFalse(session.matches("news", 3));
        assertFalse(session.matches("weather", 2));
    }

    @Test
    public void noneSessionNeverMatchesCallbacks() {
        ToolSession session = ToolSession.none();

        assertEquals("", session.question());
        assertFalse(session.realtimeMode());
        assertEquals(0, session.token());
        assertEquals("", session.toolId());
        assertFalse(session.isRealtimeTool("news"));
        assertFalse(session.matches("news", 0));
    }
}
