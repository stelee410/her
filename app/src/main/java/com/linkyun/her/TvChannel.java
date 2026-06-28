package com.linkyun.her;

final class TvChannel {
    final String id;
    final String title;
    final String subtitle;
    final String uri;
    final boolean live;

    TvChannel(String id, String title, String subtitle, String uri, boolean live) {
        this.id = id == null ? "" : id;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.uri = uri == null ? "" : uri;
        this.live = live;
    }
}
