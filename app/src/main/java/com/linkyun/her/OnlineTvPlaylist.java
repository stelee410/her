package com.linkyun.her;

import java.util.ArrayList;
import java.util.List;

final class OnlineTvPlaylist {
    static final String DEFAULT_FINANCE_CHANNEL_ID = "cgtn-global-biz";

    private OnlineTvPlaylist() {
    }

    static List<TvChannel> channels() {
        List<TvChannel> channels = new ArrayList<>();
        channels.add(channel("xg-mp4", "火山样片", "国内 CDN · MP4",
                "https://sf1-cdn-tos.huoshanstatic.com/obj/media-fe/xgplayer_doc_video/mp4/xgplayer-demo-360p.mp4",
                false));
        channels.add(channel("xg-hls", "火山 HLS", "国内 CDN · HLS",
                "https://sf1-cdn-tos.huoshanstatic.com/obj/media-fe/xgplayer_doc_video/hls/xgplayer-demo.m3u8",
                false));
        channels.add(channel("cgtn-global-biz", "CGTN Global Biz", "财经商业 · HLS",
                "https://amg01314-amg01314c6-distrotv-us-10220.playouts.now.amagi.tv/playlist/amg01314-cgtn-cgtnglobalbiz-distrotvus/playlist.m3u8",
                true));
        channels.add(channel("alarabiya-business", "Al Arabiya Business", "财经直播 · HLS",
                "https://live.alarabiya.net/alarabiapublish/aswaaq.smil/playlist.m3u8",
                true));
        channels.add(channel("asharq-business", "Asharq Business", "财经直播 · HLS",
                "https://live-news.asharq.com/asharq.m3u8",
                true));
        channels.add(channel("business-today", "Business Today TV", "财经新闻 · HLS",
                "https://feeds.intoday.in/bttv/itgd.m3u8",
                true));
        channels.add(channel("ameritrade", "Ameritrade", "市场资讯 · HLS",
                "https://tdameritrade-vizio.amagi.tv/playlist.m3u8",
                true));
        channels.add(channel("cctvplus", "CCTV+", "公开直播 · HLS",
                "https://cd-live-stream.news.cctvplus.com/live/smil:CHANNEL1.smil/playlist.m3u8",
                true));
        channels.add(channel("mux-test", "Mux Stream", "备用测试 · HLS",
                "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                false));
        channels.add(channel("w3c-sintel", "Sintel", "备用测试 · MP4",
                "https://media.w3.org/2010/05/sintel/trailer.mp4",
                false));
        return channels;
    }

    static int indexOfChannel(List<TvChannel> channels, String channelId) {
        if (channels == null || channelId == null || channelId.trim().isEmpty()) return -1;
        for (int i = 0; i < channels.size(); i++) {
            if (channelId.equals(channels.get(i).id)) return i;
        }
        return -1;
    }

    private static TvChannel channel(String id, String title, String subtitle, String url, boolean live) {
        return new TvChannel(id, title, subtitle, url, live);
    }
}
