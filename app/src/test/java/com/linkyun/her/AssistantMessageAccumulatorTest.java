package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class AssistantMessageAccumulatorTest {
    @Test
    public void appendDeltaCreatesOneAssistantAndAccumulatesText() {
        List<Message> messages = new ArrayList<>();
        AssistantMessageAccumulator accumulator = new AssistantMessageAccumulator(
                messages, role -> role + "-1");

        assertTrue(accumulator.appendDelta("你"));
        assertTrue(accumulator.appendDelta("好"));

        assertEquals(1, messages.size());
        assertEquals("assistant-1", messages.get(0).id);
        assertEquals("assistant", messages.get(0).role);
        assertEquals("你好", messages.get(0).text);
        assertEquals(messages.get(0), accumulator.activeMessage());
    }

    @Test
    public void clearActiveKeepsMessageButStopsFurtherAccumulationIntoIt() {
        List<Message> messages = new ArrayList<>();
        final int[] next = {1};
        AssistantMessageAccumulator accumulator = new AssistantMessageAccumulator(
                messages, role -> role + "-" + next[0]++);

        accumulator.appendDelta("第一句");
        accumulator.clearActive();
        accumulator.appendDelta("第二句");

        assertEquals(2, messages.size());
        assertEquals("第一句", messages.get(0).text);
        assertEquals("第二句", messages.get(1).text);
    }

    @Test
    public void discardActiveRemovesOnlyTheActiveAssistantDraft() {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("u1", "user", "问天气"));
        AssistantMessageAccumulator accumulator = new AssistantMessageAccumulator(
                messages, role -> "a1");

        accumulator.appendDelta("半句回复");

        assertTrue(accumulator.discardActive());
        assertEquals(1, messages.size());
        assertEquals("u1", messages.get(0).id);
        assertNull(accumulator.activeMessage());
        assertFalse(accumulator.discardActive());
    }

    @Test
    public void removeAssistantReplyAfterLastUserKeepsToolCardsAndOlderAssistant() {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("a1", "assistant", "旧回复"));
        messages.add(new Message("u1", "user", "最新问题"));
        messages.add(new Message("t1", "tool", "工具卡片"));
        messages.add(new Message("a2", "assistant", "要删掉"));
        AssistantMessageAccumulator accumulator = new AssistantMessageAccumulator(
                messages, role -> "a3");

        assertTrue(accumulator.removeAssistantReplyAfterLastUser());

        assertEquals(3, messages.size());
        assertEquals("a1", messages.get(0).id);
        assertEquals("u1", messages.get(1).id);
        assertEquals("t1", messages.get(2).id);
        assertNull(accumulator.activeMessage());
    }
}
