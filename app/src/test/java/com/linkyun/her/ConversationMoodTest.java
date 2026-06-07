package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ConversationMoodTest {
    @Test
    public void detectsAnxiousOrSadText() {
        assertEquals(1, ConversationMood.forText("我今天有点焦虑"));
        assertEquals(1, ConversationMood.forText("sad and anxious"));
    }

    @Test
    public void detectsWarmPositiveText() {
        assertEquals(2, ConversationMood.forText("今天很开心"));
        assertEquals(2, ConversationMood.forText("I love this"));
    }

    @Test
    public void detectsQuietOrTiredText() {
        assertEquals(3, ConversationMood.forText("我有点累"));
        assertEquals(3, ConversationMood.forText("quiet evening"));
    }

    @Test
    public void neutralForBlankOrUnknownText() {
        assertEquals(0, ConversationMood.forText(null));
        assertEquals(0, ConversationMood.forText("普通的一句话"));
    }
}
