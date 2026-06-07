package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class ConversationHistoryTest {
    @Test
    public void lastAssistantBeforeLatestUserSkipsCurrentTailAndTools() {
        List<Message> messages = Arrays.asList(
                new Message("a1", "assistant", "第一问"),
                new Message("u1", "user", "我叫史蒂芬"),
                new Message("t1", "tool", "工具卡片"),
                new Message("a2", "assistant", "第二问"),
                new Message("u2", "user", "女朋友"));

        assertEquals("第二问", ConversationHistory.lastAssistantBeforeLatestUser(messages));
    }

    @Test
    public void lastAssistantBeforeLatestUserReturnsEmptyWhenNoPreviousAssistant() {
        List<Message> messages = Arrays.asList(
                new Message("u1", "user", "hello"),
                new Message("t1", "tool", "工具卡片"));

        assertEquals("", ConversationHistory.lastAssistantBeforeLatestUser(messages));
        assertEquals("", ConversationHistory.lastAssistantBeforeLatestUser(null));
    }

    @Test
    public void recentDialogueKeepsOnlyRecentUserAndAssistantMessages() {
        List<Message> messages = Arrays.asList(
                new Message("u1", "user", " old "),
                new Message("a1", "assistant", "answer"),
                new Message("t1", "tool", "card"),
                new Message("u2", "user", "  "),
                new Message("u3", "user", "new"));

        assertEquals("assistant: answer\nuser: new\n", ConversationHistory.recentDialogue(messages, 4));
    }

    @Test
    public void recentDialogueReturnsEmptyForNoLimit() {
        assertEquals("", ConversationHistory.recentDialogue(Arrays.asList(
                new Message("u1", "user", "hello")), 0));
    }

    @Test
    public void initializationTranscriptIncludesAllMessageRolesInOrder() {
        Message nullText = new Message("a2", "assistant", null);
        List<Message> messages = Arrays.asList(
                new Message("a1", "assistant", "hello"),
                null,
                new Message("t1", "tool", "card"),
                new Message("u1", "user", "name"),
                nullText);

        assertEquals("assistant: hello\ntool: card\nuser: name\nassistant: null\n",
                ConversationHistory.initializationTranscript(messages));
        assertEquals("", ConversationHistory.initializationTranscript(null));
    }

    @Test
    public void lastConversationLineUsesLatestUserOrAssistantAndFallback() {
        List<Message> messages = Arrays.asList(
                new Message("u1", "user", "hello"),
                new Message("t1", "tool", "tool text"),
                new Message("a1", "assistant", " answer "));

        assertEquals("answer", ConversationHistory.lastConversationLine(messages, "fallback"));
        assertEquals("fallback", ConversationHistory.lastConversationLine(Arrays.asList(
                new Message("t1", "tool", "tool text")), "fallback"));
        assertEquals("", ConversationHistory.lastConversationLine(null, null));
    }

    @Test
    public void lastAnyLineAllowsInitializationSystemMessages() {
        List<Message> messages = Arrays.asList(
                new Message("u1", "user", "hello"),
                new Message("t1", "tool", " tool text "));

        assertEquals("tool text", ConversationHistory.lastAnyLine(messages, "fallback"));
        assertEquals("fallback", ConversationHistory.lastAnyLine(Arrays.asList(
                new Message("empty", "assistant", "   ")), "fallback"));
    }
}
