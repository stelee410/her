package com.linkyun.her;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

final class WeatherCard {
    private WeatherCard() { }

    static View chat(MainActivity activity, HerUi ui, Message message) {
        WeatherTool.WeatherResult weather = message.weather;
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setGravity(Gravity.LEFT);

        LinearLayout card = baseCard(ui, 18, 16, 18, 14);
        card.addView(header(activity, ui, weather, 34, 48, 18, 36, 102, 58), new LinearLayout.LayoutParams(-1, -2));

        LinearLayout metrics = metrics(activity, ui, weather, 14);
        card.addView(metrics, new LinearLayout.LayoutParams(-1, -2));

        TextView time = ui.text(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(new Date(message.timestamp)), 12, 0x80FFE0E0, 0);
        time.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), 0);
        card.addView(time, new LinearLayout.LayoutParams(-1, -2));

        int width = activity.getResources().getDisplayMetrics().widthPixels - ui.dp(70);
        wrap.addView(card, new LinearLayout.LayoutParams(Math.max(ui.dp(260), width), -2));
        return wrap;
    }

    static View voice(MainActivity activity, HerUi ui, WeatherTool.WeatherResult weather) {
        LinearLayout card = baseCard(ui, 18, 15, 18, 14);
        card.setElevation(ui.dp(10));
        card.addView(header(activity, ui, weather, 32, 46, 17, 34, 96, 54), new LinearLayout.LayoutParams(-1, -2));
        card.addView(metrics(activity, ui, weather, 12), new LinearLayout.LayoutParams(-1, -2));
        return card;
    }

    private static LinearLayout baseCard(HerUi ui, int left, int top, int right, int bottom) {
        LinearLayout card = new LinearLayout(ui.activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(ui.dp(left), ui.dp(top), ui.dp(right), ui.dp(bottom));
        card.setBackground(ui.bubbleDrawable(false));
        return card;
    }

    private static LinearLayout header(MainActivity activity, HerUi ui, WeatherTool.WeatherResult weather,
            int glyphSize, int glyphBox, int placeSize, int tempSize, int tempWidth, int tempHeight) {
        LinearLayout header = new LinearLayout(activity);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView glyph = ui.text(weatherGlyph(weather.condition), glyphSize, Color.WHITE, 0);
        glyph.setGravity(Gravity.CENTER);
        header.addView(glyph, new LinearLayout.LayoutParams(ui.dp(glyphBox), ui.dp(glyphBox)));

        LinearLayout titleColumn = new LinearLayout(activity);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        TextView place = ui.text(weather.placeName, placeSize, Color.WHITE, 600);
        TextView condition = ui.text(weather.condition + " · " + weather.observedAt, 12, 0xA8FFE0E0, 0);
        titleColumn.addView(place, new LinearLayout.LayoutParams(-1, -2));
        titleColumn.addView(condition, new LinearLayout.LayoutParams(-1, -2));
        header.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1));

        TextView temp = ui.text(weather.temperatureText(), tempSize, Color.WHITE, 0);
        temp.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(temp, new LinearLayout.LayoutParams(ui.dp(tempWidth), ui.dp(tempHeight)));
        return header;
    }

    private static LinearLayout metrics(MainActivity activity, HerUi ui, WeatherTool.WeatherResult weather, int topPadding) {
        LinearLayout metrics = new LinearLayout(activity);
        metrics.setGravity(Gravity.CENTER);
        metrics.setPadding(0, ui.dp(topPadding), 0, 0);
        metrics.addView(metric(ui, "体感", weather.apparentTemperatureText()), new LinearLayout.LayoutParams(0, -2, 1));
        metrics.addView(metric(ui, "湿度", weather.humidityText()), new LinearLayout.LayoutParams(0, -2, 1));
        metrics.addView(metric(ui, "风速", weather.windText()), new LinearLayout.LayoutParams(0, -2, 1));
        return metrics;
    }

    private static LinearLayout metric(HerUi ui, String label, String value) {
        LinearLayout column = new LinearLayout(ui.activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        TextView top = ui.text(label, 11, 0x99FFE0E0, 0);
        top.setGravity(Gravity.CENTER);
        TextView bottom = ui.text(value, 14, Color.WHITE, 600);
        bottom.setGravity(Gravity.CENTER);
        column.addView(top, new LinearLayout.LayoutParams(-1, -2));
        column.addView(bottom, new LinearLayout.LayoutParams(-1, -2));
        return column;
    }

    private static String weatherGlyph(String condition) {
        if (condition == null) return "☼";
        if (condition.contains("雨")) return "☂";
        if (condition.contains("雪")) return "❄";
        if (condition.contains("雾")) return "≋";
        if (condition.contains("阴")) return "☁";
        if (condition.contains("云")) return "☁";
        return "☼";
    }
}
