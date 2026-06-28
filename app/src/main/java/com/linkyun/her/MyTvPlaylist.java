package com.linkyun.her;

import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class MyTvPlaylist {
    private static final String DIRECTORY_NAME = "mytv";
    private static final String[] VIDEO_EXTENSIONS = {
            ".mp4", ".m4v", ".3gp", ".webm", ".mkv"
    };

    private MyTvPlaylist() {
    }

    static File directory() {
        return new File(Environment.getExternalStorageDirectory(), DIRECTORY_NAME);
    }

    static boolean ensureDirectory() {
        File directory = directory();
        return directory.exists() || directory.mkdirs();
    }

    static List<File> videos() {
        File[] files = directory().listFiles(file -> file.isFile() && isVideo(file.getName()));
        List<File> result = new ArrayList<>();
        if (files == null) return result;
        Collections.addAll(result, files);
        result.sort(NATURAL_FILE_ORDER);
        return result;
    }

    static List<TvChannel> channels() {
        List<File> videos = videos();
        List<TvChannel> channels = new ArrayList<>();
        for (int i = 0; i < videos.size(); i++) {
            File video = videos.get(i);
            channels.add(new TvChannel("local-" + i, localTitle(video),
                    "本地视频 · " + displayPath(), video.toURI().toString(), false));
        }
        return channels;
    }

    static String displayPath() {
        return directory().getAbsolutePath();
    }

    static boolean isVideo(String name) {
        String lower = name == null ? "" : name.toLowerCase(Locale.US);
        for (String extension : VIDEO_EXTENSIONS) {
            if (lower.endsWith(extension)) return true;
        }
        return false;
    }

    private static String localTitle(File file) {
        String name = file == null ? "" : file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static final Comparator<File> NATURAL_FILE_ORDER =
            (left, right) -> compareNatural(left.getName(), right.getName());

    static int compareNatural(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        int ai = 0;
        int bi = 0;
        while (ai < a.length() && bi < b.length()) {
            char ac = a.charAt(ai);
            char bc = b.charAt(bi);
            if (Character.isDigit(ac) && Character.isDigit(bc)) {
                int aStart = ai;
                int bStart = bi;
                while (ai < a.length() && Character.isDigit(a.charAt(ai))) ai++;
                while (bi < b.length() && Character.isDigit(b.charAt(bi))) bi++;
                String aNumber = trimLeadingZeroes(a.substring(aStart, ai));
                String bNumber = trimLeadingZeroes(b.substring(bStart, bi));
                if (aNumber.length() != bNumber.length()) {
                    return aNumber.length() - bNumber.length();
                }
                int numberCompare = aNumber.compareTo(bNumber);
                if (numberCompare != 0) return numberCompare;
                continue;
            }
            int charCompare = Character.toLowerCase(ac) - Character.toLowerCase(bc);
            if (charCompare != 0) return charCompare;
            ai++;
            bi++;
        }
        return a.length() - b.length();
    }

    private static String trimLeadingZeroes(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') index++;
        return value.substring(index);
    }
}
