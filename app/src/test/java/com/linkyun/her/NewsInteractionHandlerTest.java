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

    @Test
    public void blankErrorBuildsGenericFailureResult() {
        NewsInteractionHandler handler = new NewsInteractionHandler(
                callback -> callback.onError(" \u00A0 "));
        List<ToolInteractionResult<NewsTool.NewsResult>> results = new ArrayList<>();

        handler.fetch("新闻", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<NewsTool.NewsResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("工具异常", result.errorMessage);
        assertTrue(result.fact.contains("读取失败：工具异常"));
        assertEquals("暂时没读到新闻热点，工具异常。你可以稍后再试一下。", result.answer);
    }

    @Test
    public void nullSuccessBuildsFailureResultInsteadOfCrashing() {
        NewsInteractionHandler handler = new NewsInteractionHandler(callback -> callback.onSuccess(null));
        List<ToolInteractionResult<NewsTool.NewsResult>> results = new ArrayList<>();

        handler.fetch("新闻", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<NewsTool.NewsResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("新闻", result.question);
        assertEquals("新闻结果为空", result.errorMessage);
        assertTrue(result.fact.contains("读取失败：新闻结果为空"));
        assertEquals("暂时没读到新闻热点，新闻结果为空。你可以稍后再试一下。", result.answer);
    }

    @Test
    public void emptySuccessBuildsSafeResultInsteadOfCrashing() {
        NewsTool.NewsResult empty = new NewsTool.NewsResult("source", null);
        NewsInteractionHandler handler = new NewsInteractionHandler(callback -> callback.onSuccess(empty));
        List<ToolInteractionResult<NewsTool.NewsResult>> results = new ArrayList<>();

        handler.fetch("新闻", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<NewsTool.NewsResult> result = results.get(0);
        assertTrue(result.success);
        assertEquals("新闻", result.question);
        assertTrue(result.fact.contains("来源：source"));
        assertEquals("今天 agentNews 上的热点有：", result.answer);
    }

    @Test
    public void sourceExceptionBuildsFailureResultInsteadOfCrashing() {
        NewsInteractionHandler handler = new NewsInteractionHandler(callback -> {
            throw new RuntimeException("source boom");
        });
        List<ToolInteractionResult<NewsTool.NewsResult>> results = new ArrayList<>();

        handler.fetch("新闻", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<NewsTool.NewsResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("新闻", result.question);
        assertEquals("source boom", result.errorMessage);
        assertTrue(result.fact.contains("读取失败：source boom"));
        assertEquals("暂时没读到新闻热点，source boom。你可以稍后再试一下。", result.answer);
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
