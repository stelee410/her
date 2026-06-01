package com.linkyun.her;

final class Message {
    final String id;
    final String role;
    String text;
    WeatherTool.WeatherResult weather;
    NewsTool.NewsResult news;
    final long timestamp = System.currentTimeMillis();

    Message(String id, String role, String text) {
        this.id = id;
        this.role = role;
        this.text = text;
    }

    Message(String id, WeatherTool.WeatherResult weather) {
        this.id = id;
        this.role = "tool";
        this.text = "";
        this.weather = weather;
    }

    Message(String id, NewsTool.NewsResult news) {
        this.id = id;
        this.role = "tool";
        this.text = "";
        this.news = news;
    }
}
