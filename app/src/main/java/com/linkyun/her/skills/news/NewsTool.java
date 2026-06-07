package com.linkyun.her;

import android.os.Handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class NewsTool {
    private static final String BASE_URL = "https://agentnews.linkyun.co";
    private static final String SOURCE_URL = BASE_URL + "/api/v1/feed?lang=zh&limit=8&format=json";
    private static final int MAX_ITEMS = 8;

    interface CallbackResult {
        void onSuccess(NewsResult result);
        void onError(String message);
    }

    private final OkHttpClient http;
    private final Handler main;

    NewsTool(OkHttpClient http, Handler main) {
        this.http = http;
        this.main = main;
    }

    void fetchDaily(CallbackResult callback) {
        Request request = new Request.Builder()
                .url(SOURCE_URL)
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .build();
        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> callback.onError("新闻热点读取失败：" + error.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    main.post(() -> callback.onError("新闻热点接口失败：" + response.code()));
                    return;
                }
                List<NewsItem> items;
                try {
                    items = parseFeedItems(body);
                } catch (JSONException error) {
                    main.post(() -> callback.onError("新闻热点解析失败：" + error.getMessage()));
                    return;
                }
                if (items.isEmpty()) {
                    main.post(() -> callback.onError("暂时没有读到新闻热点。"));
                    return;
                }
                NewsResult result = new NewsResult(SOURCE_URL, items);
                main.post(() -> callback.onSuccess(result));
            }
        });
    }

    static List<NewsItem> parseFeedItems(String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        JSONObject feed = new JSONObject(json);
        JSONArray array = feed.optJSONArray("items");
        if (array == null) return Collections.emptyList();
        List<NewsItem> items = new ArrayList<>();
        for (int i = 0; i < array.length() && items.size() < MAX_ITEMS; i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            String title = cleanText(item.optString("title", ""));
            if (title.isEmpty()) continue;
            items.add(new NewsItem(
                    title,
                    cleanText(item.optString("summary", "")),
                    dateFromUpdatedAt(item.optString("updated_at", "")),
                    cleanText(item.optString("type", "")),
                    tagsFromJson(item.optJSONArray("tags")),
                    normalizeUrl(item.optString("get", ""))));
        }
        return items;
    }

    private static String normalizeUrl(String href) {
        if (href == null) return SOURCE_URL;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("/")) return BASE_URL + href;
        return BASE_URL + "/" + href;
    }

    private static String dateFromUpdatedAt(String updatedAt) {
        String value = cleanText(updatedAt);
        int split = value.indexOf('T');
        return split > 0 ? value.substring(0, split) : value;
    }

    private static List<String> tagsFromJson(JSONArray array) {
        if (array == null) return Collections.emptyList();
        List<String> tags = new ArrayList<>();
        for (int i = 0; i < array.length() && tags.size() < 5; i++) {
            String tag = cleanText(array.optString(i, ""));
            if (!tag.isEmpty()) tags.add(tag);
        }
        return tags;
    }

    private static String cleanText(String value) {
        if (value == null) return "";
        return value.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    static final class NewsResult {
        final String sourceUrl;
        final List<NewsItem> items;

        NewsResult(String sourceUrl, List<NewsItem> items) {
            this.sourceUrl = cleanText(sourceUrl);
            this.items = cleanItems(items);
        }

        String fact(String userQuestion) {
            StringBuilder builder = new StringBuilder();
            builder.append("【每日新闻热点结果】用户刚才问：").append(userQuestion).append('\n');
            builder.append("来源：").append(sourceUrl).append('\n');
            builder.append("以下条目也是新闻卡片显示的完整列表。请只按这个编号列表回答，不要补充列表外新闻。\n");
            for (int i = 0; i < items.size(); i++) {
                NewsItem item = items.get(i);
                builder.append(i + 1).append(". ").append(item.title);
                String meta = item.metaText();
                if (!meta.isEmpty()) builder.append("（").append(meta).append("）");
                builder.append('\n');
                if (!item.summary.isEmpty()) builder.append("摘要：").append(item.summary).append('\n');
            }
            builder.append("请直接用自然中文概括或播报上面这些每日新闻热点，不要说你不能查新闻，也不要提工具调用。");
            return builder.toString();
        }

        String shortAnswer() {
            StringBuilder builder = new StringBuilder("今天 agentNews 上的热点有：");
            for (int i = 0; i < items.size(); i++) {
                builder.append('\n').append(i + 1).append(". ").append(items.get(i).title);
            }
            return builder.toString();
        }

        String fetchedDateText() {
            if (items.isEmpty() || items.get(0).date.isEmpty()) return "";
            return items.get(0).date;
        }
    }

    static final class NewsItem {
        final String title;
        final String summary;
        final String date;
        final String category;
        final List<String> tags;
        final String url;

        NewsItem(String title, String summary, String date, String category, List<String> tags, String url) {
            this.title = cleanText(title);
            this.summary = cleanText(summary);
            this.date = cleanText(date);
            this.category = cleanText(category);
            this.tags = cleanTags(tags);
            this.url = cleanText(url);
        }

        String metaText() {
            if (!date.isEmpty() && !category.isEmpty()) return date + " · " + category;
            if (!date.isEmpty()) return date;
            return category;
        }

        String tagText() {
            if (tags.isEmpty()) return "";
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) builder.append(' ');
                builder.append(tags.get(i));
            }
            return builder.toString();
        }
    }

    private static List<NewsItem> cleanItems(List<NewsItem> items) {
        if (items == null || items.isEmpty()) return Collections.emptyList();
        List<NewsItem> cleaned = new ArrayList<>();
        for (NewsItem item : items) {
            if (item != null) cleaned.add(item);
        }
        return cleaned.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(cleaned);
    }

    private static List<String> cleanTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptyList();
        List<String> cleaned = new ArrayList<>();
        for (String tag : tags) {
            String clean = cleanText(tag);
            if (!clean.isEmpty()) cleaned.add(clean);
        }
        return cleaned.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(cleaned);
    }
}
