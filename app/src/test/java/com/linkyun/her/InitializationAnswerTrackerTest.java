package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InitializationAnswerTrackerTest {
    @Test
    public void smallTalkAndShortConfirmationDoNotAdvance() {
        assertEquals(0, InitializationAnswerTracker.nextTurn(0, "你叫什么？", "你好"));
        assertEquals(1, InitializationAnswerTracker.nextTurn(1, "你希望和我建立什么关系？", "好的"));
        assertEquals(2, InitializationAnswerTracker.nextTurn(2, "讲讲你的故事", "嗯嗯"));
    }

    @Test
    public void firstTurnAdvancesFromAssistantQuestionOrNameCue() {
        assertEquals(1, InitializationAnswerTracker.nextTurn(0, "你叫什么？", "史蒂芬"));
        assertEquals(1, InitializationAnswerTracker.nextTurn(0, "", "我叫史蒂芬"));
        assertEquals(0, InitializationAnswerTracker.nextTurn(0, "", "最近还不错"));
    }

    @Test
    public void secondTurnAdvancesFromRelationshipQuestionOrRelationshipText() {
        assertEquals(2, InitializationAnswerTracker.nextTurn(1, "你希望和我建立什么关系？", "女朋友"));
        assertEquals(2, InitializationAnswerTracker.nextTurn(1, "", "我希望你像朋友一样"));
        assertEquals(1, InitializationAnswerTracker.nextTurn(1, "", "最近还不错"));
    }

    @Test
    public void thirdTurnAdvancesFromStoryQuestionOrLikelyStory() {
        assertEquals(3, InitializationAnswerTracker.nextTurn(2, "讲讲你的故事", "最近我在做一个产品"));
        assertEquals(3, InitializationAnswerTracker.nextTurn(2, "", "我最近在创业，有点忙但也挺期待"));
        assertEquals(2, InitializationAnswerTracker.nextTurn(2, "", "女朋友"));
        assertEquals(2, InitializationAnswerTracker.nextTurn(2, "", "好"));
    }

    @Test
    public void helperClassifiesQuestionsAndRelationships() {
        assertTrue(InitializationAnswerTracker.asksForUserName("我应该怎么称呼你？"));
        assertTrue(InitializationAnswerTracker.asksForRelationship("你希望和我建立什么关系？"));
        assertTrue(InitializationAnswerTracker.asksForStory("讲讲你的故事吧"));
        assertTrue(InitializationAnswerTracker.hasNameCue("你可以叫我小林"));
        assertEquals("朋友和助理", InitializationAnswerTracker.relationshipFromText("我希望你是朋友和助手"));
        assertFalse(InitializationAnswerTracker.isLikelyStoryAnswer("女朋友"));
    }
}
