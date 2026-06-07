package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class NewsToolTest {
    @Test
    public void parsesStructuredFeedItems() throws Exception {
        String json = "{"
                + "\"lang\":\"zh\","
                + "\"items\":[{"
                + "\"id\":\"a\","
                + "\"type\":\"ai-news\","
                + "\"title\":\"AI 新闻\","
                + "\"summary\":\"摘要 内容\","
                + "\"tags\":[\"ai\",\"agent\",\"extra\",\"four\",\"five\",\"ignored\"],"
                + "\"updated_at\":\"2026-06-03T11:59:10.807Z\","
                + "\"get\":\"/api/v1/articles/a?lang=zh\""
                + "}]"
                + "}";

        List<NewsTool.NewsItem> items = NewsTool.parseFeedItems(json);

        assertEquals(1, items.size());
        NewsTool.NewsItem item = items.get(0);
        assertEquals("AI 新闻", item.title);
        assertEquals("摘要 内容", item.summary);
        assertEquals("2026-06-03", item.date);
        assertEquals("ai-news", item.category);
        assertEquals("https://agentnews.linkyun.co/api/v1/articles/a?lang=zh", item.url);
        assertEquals("ai agent extra four five", item.tagText());
    }

    @Test
    public void skipsItemsWithoutTitleAndLimitsResultCount() throws Exception {
        StringBuilder json = new StringBuilder("{\"items\":[{\"summary\":\"missing\"}");
        for (int i = 0; i < 10; i++) {
            json.append(",{\"title\":\"news ").append(i).append("\"}");
        }
        json.append("]}");

        List<NewsTool.NewsItem> items = NewsTool.parseFeedItems(json.toString());

        assertEquals(8, items.size());
        assertEquals("news 0", items.get(0).title);
        assertEquals("news 7", items.get(7).title);
    }

    @Test
    public void resultAndItemConstructorsNormalizeNullFields() {
        NewsTool.NewsItem item = new NewsTool.NewsItem(null, null, null, null, null, null);
        NewsTool.NewsResult result = new NewsTool.NewsResult(null, null);

        assertEquals("", item.title);
        assertEquals("", item.summary);
        assertEquals("", item.metaText());
        assertEquals("", item.tagText());
        assertEquals("", item.url);
        assertTrue(result.sourceUrl.isEmpty());
        assertTrue(result.items.isEmpty());
    }

    @Test
    public void resultFiltersNullItemsAndItemFiltersBlankTags() {
        NewsTool.NewsItem item = new NewsTool.NewsItem(
                "title",
                "",
                "",
                "",
                Arrays.asList(" ai ", null, " ", "\u00A0agent"),
                "");
        NewsTool.NewsResult result = new NewsTool.NewsResult("source", Arrays.asList(null, item, null));

        assertEquals(1, result.items.size());
        assertEquals(item, result.items.get(0));
        assertEquals("ai agent", item.tagText());
        assertTrue(result.fact("新闻").contains("1. title"));
        assertEquals("今天 agentNews 上的热点有：\n1. title", result.shortAnswer());
    }

    @Test
    public void failureFactUsesGenericMessageForBlankInput() {
        assertTrue(NewsSkill.failureFact(" \u00A0 ").contains("读取失败：工具异常"));
        assertEquals("工具异常", NewsSkill.failureMessage(null));
    }
}
