package com.linkyun.her;

import static org.junit.Assert.assertEquals;

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
}
