package com.linkyun.her;

import android.os.Handler;
import android.text.Html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class NewsTool {
    private static final String SOURCE_URL = "https://agentnews.linkyun.co/";
    private static final int MAX_ITEMS = 8;
    private static final Pattern CARD_START_PATTERN = Pattern.compile(
            "<a\\s+class=\"card\"\\s+href=\"([^\"]+)\"[^>]*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "<h2>(.*?)</h2>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile(
            "<p\\s+class=\"summary\"[^>]*>(.*?)</p>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_PATTERN = Pattern.compile(
            "<div\\s+class=\"meta\"[^>]*>(.*?)</div>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern PILL_PATTERN = Pattern.compile(
            "<span\\s+class=\"pill\"[^>]*>(.*?)</span>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "<a\\s+class=\"tag\"[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

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
                List<NewsItem> items = parseItems(body);
                if (items.isEmpty()) {
                    main.post(() -> callback.onError("暂时没有读到新闻热点。"));
                    return;
                }
                NewsResult result = new NewsResult(SOURCE_URL, items);
                main.post(() -> callback.onSuccess(result));
            }
        });
    }

    private static List<NewsItem> parseItems(String html) {
        if (html == null || html.trim().isEmpty()) return Collections.emptyList();
        List<NewsItem> items = new ArrayList<>();
        Matcher matcher = CARD_START_PATTERN.matcher(html);
        int searchFrom = 0;
        while (matcher.find(searchFrom) && items.size() < MAX_ITEMS) {
            String url = normalizeUrl(matcher.group(1));
            int contentStart = matcher.end();
            int contentEnd = nextCardStart(html, contentStart);
            String card = html.substring(contentStart, contentEnd);
            String title = firstCleanMatch(TITLE_PATTERN, card);
            String summary = firstCleanMatch(SUMMARY_PATTERN, card);
            String metaHtml = firstRawMatch(META_PATTERN, card);
            searchFrom = contentEnd;
            if (title.isEmpty()) continue;
            items.add(new NewsItem(
                    title,
                    summary,
                    dateFromMeta(metaHtml),
                    categoryFromMeta(metaHtml),
                    tagsFromMeta(metaHtml),
                    url));
        }
        return items;
    }

    private static int nextCardStart(String html, int from) {
        Matcher next = CARD_START_PATTERN.matcher(html);
        if (next.find(from)) return next.start();
        int mainEnd = html.indexOf("</main>", from);
        return mainEnd >= 0 ? mainEnd : html.length();
    }

    private static String normalizeUrl(String href) {
        if (href == null) return SOURCE_URL;
        if (href.startsWith("http://") || href.startsWith("https://")) return href;
        if (href.startsWith("/")) return "https://agentnews.linkyun.co" + href;
        return SOURCE_URL + href;
    }

    private static String firstRawMatch(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String firstCleanMatch(Pattern pattern, String value) {
        return cleanHtml(firstRawMatch(pattern, value));
    }

    private static String dateFromMeta(String metaHtml) {
        String meta = cleanHtml(metaHtml);
        int split = meta.indexOf('·');
        return split > 0 ? meta.substring(0, split).trim() : "";
    }

    private static String categoryFromMeta(String metaHtml) {
        Matcher matcher = PILL_PATTERN.matcher(metaHtml == null ? "" : metaHtml);
        return matcher.find() ? cleanHtml(matcher.group(1)) : "";
    }

    private static List<String> tagsFromMeta(String metaHtml) {
        List<String> tags = new ArrayList<>();
        Matcher matcher = TAG_PATTERN.matcher(metaHtml == null ? "" : metaHtml);
        while (matcher.find() && tags.size() < 5) {
            String tag = cleanHtml(matcher.group(1));
            if (!tag.isEmpty()) tags.add(tag);
        }
        return tags;
    }

    private static String cleanHtml(String value) {
        if (value == null) return "";
        return Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    static final class NewsResult {
        final String sourceUrl;
        final List<NewsItem> items;

        NewsResult(String sourceUrl, List<NewsItem> items) {
            this.sourceUrl = sourceUrl;
            this.items = items;
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
            this.title = title;
            this.summary = summary;
            this.date = date;
            this.category = category;
            this.tags = tags;
            this.url = url;
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
}
