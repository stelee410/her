package com.linkyun.her;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

final class NewsCard {
    private NewsCard() { }

    static View chat(MainActivity activity, HerUi ui, Message message) {
        NewsTool.NewsResult news = message.news;
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setGravity(Gravity.LEFT);

        LinearLayout card = baseCard(ui, 18, 16, 18, 14);
        card.addView(header(ui, news, 22, 15), new LinearLayout.LayoutParams(-1, -2));
        card.addView(scrollableItems(activity, ui, news, true), new LinearLayout.LayoutParams(-1, scrollHeight(activity, ui, true)));

        TextView time = ui.text(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(new Date(message.timestamp)), 12, 0x80FFE0E0, 0);
        time.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), 0);
        card.addView(time, new LinearLayout.LayoutParams(-1, -2));

        int width = activity.getResources().getDisplayMetrics().widthPixels - ui.dp(70);
        wrap.addView(card, new LinearLayout.LayoutParams(Math.max(ui.dp(260), width), -2));
        return wrap;
    }

    static View voice(MainActivity activity, HerUi ui, NewsTool.NewsResult news) {
        LinearLayout card = baseCard(ui, 18, 15, 18, 14);
        card.setElevation(ui.dp(10));
        card.addView(header(ui, news, 22, 15), new LinearLayout.LayoutParams(-1, -2));
        card.addView(scrollableItems(activity, ui, news, false), new LinearLayout.LayoutParams(-1, scrollHeight(activity, ui, false)));
        return card;
    }

    private static LinearLayout baseCard(HerUi ui, int left, int top, int right, int bottom) {
        LinearLayout card = new LinearLayout(ui.activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.dp(left), ui.dp(top), ui.dp(right), ui.dp(bottom));
        card.setBackground(ui.bubbleDrawable(false));
        return card;
    }

    private static LinearLayout header(HerUi ui, NewsTool.NewsResult news, int titleSize, int metaSize) {
        LinearLayout header = new LinearLayout(ui.activity);
        header.setOrientation(LinearLayout.VERTICAL);

        TextView title = ui.text("每日新闻热点", titleSize, Color.WHITE, 600);
        header.addView(title, new LinearLayout.LayoutParams(-1, -2));

        String date = news.fetchedDateText();
        TextView meta = ui.text(date.isEmpty() ? news.sourceUrl : date + " · agentNews", metaSize, 0xA8FFE0E0, 0);
        meta.setPadding(0, ui.dp(2), 0, 0);
        header.addView(meta, new LinearLayout.LayoutParams(-1, -2));
        return header;
    }

    private static ScrollView scrollableItems(MainActivity activity, HerUi ui, NewsTool.NewsResult news, boolean chat) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setNestedScrollingEnabled(true);
        scroll.setPadding(0, ui.dp(2), 0, ui.dp(2));

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        addItems(ui, content, news, chat ? 17 : 16, chat ? 15 : 14, chat ? 13 : 12);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private static int scrollHeight(MainActivity activity, HerUi ui, boolean chat) {
        int screen = activity.getResources().getDisplayMetrics().heightPixels;
        int max = chat ? ui.dp(420) : ui.dp(320);
        int room = Math.max(ui.dp(230), screen - ui.dp(chat ? 260 : 250));
        return Math.min(max, room);
    }

    private static void addItems(HerUi ui, LinearLayout card, NewsTool.NewsResult news,
            int titleSize, int summarySize, int tagSize) {
        for (int i = 0; i < news.items.size(); i++) {
            NewsTool.NewsItem item = news.items.get(i);
            LinearLayout row = new LinearLayout(ui.activity);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, ui.dp(i == 0 ? 14 : 16), 0, 0);

            TextView title = ui.text((i + 1) + ". " + item.title, titleSize, Color.WHITE, 600);
            title.setLineSpacing(ui.dp(3), 1.0f);
            row.addView(title, new LinearLayout.LayoutParams(-1, -2));

            String summary = item.summary;
            if (!summary.isEmpty()) {
                TextView detail = ui.text(summary, summarySize, 0xB8FFE0E0, 0);
                detail.setPadding(ui.dp(20), ui.dp(5), 0, 0);
                detail.setLineSpacing(ui.dp(2), 1.0f);
                row.addView(detail, new LinearLayout.LayoutParams(-1, -2));
            }

            String tagText = item.tagText();
            if (!tagText.isEmpty()) {
                TextView tags = ui.text(tagText, tagSize, 0x88FFE0E0, 0);
                tags.setPadding(ui.dp(20), ui.dp(5), 0, 0);
                row.addView(tags, new LinearLayout.LayoutParams(-1, -2));
            }

            card.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
    }
}
