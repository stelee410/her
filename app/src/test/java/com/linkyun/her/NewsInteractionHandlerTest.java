package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class NewsInteractionHandlerTest {
    @Test
    public void successBuildsUnifiedToolResult() {
        NewsTool.NewsResult news = sampleNews();
        NewsInteractionHandler handler = new NewsInteractionHandler(callback -> callback.onSuccess(news));
        List<ToolInteractionResult<NewsTool.NewsResult>> results = new ArrayList<>();

        handler.fetch(" 查新闻 ", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<NewsTool.NewsResult> result = results.get(0);
        assertTrue(result.success);
        assertEquals("news", result.tool);
        assertEquals("查新闻", result.question);
        assertSame(news, result.payload);
        assertTrue(result.fact.contains("【每日新闻热点结果】"));
        assertTrue(result.fact.contains("用户刚才问：查新闻"));
        assertTrue(result.answer.contains("今天 agentNews 上的热点有"));
        assertEquals("", result.errorMessage);
    }

    @Test
    public void errorBuildsFailureResult() {
        NewsInteractionHandler handler = new NewsInteractionHandler(
                callback -> callback.onError("网络失败"));
        List<ToolInteractionResult<NewsTool.NewsResult>> results = new ArrayList<>();

        handler.fetch("", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<NewsTool.NewsResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("每日新闻热点", result.question);
        assertEquals("网络失败", result.errorMessage);
        assertTrue(result.fact.contains("读取失败：网络失败"));
        assertEquals("暂时没读到新闻热点，网络失败。你可以稍后再试一下。", result.answer);
    }

    private static NewsTool.NewsResult sampleNews() {
        return new NewsTool.NewsResult("https://agentnews.linkyun.co/api/v1/feed?lang=zh&limit=8&format=json",
                Arrays.asList(new NewsTool.NewsItem(
                        "新闻标题",
                        "新闻摘要",
                        "2026-06-05",
                        "ai-news",
                        Arrays.asList("ai", "agent"),
                        "https://agentnews.linkyun.co/a")));
    }
}
