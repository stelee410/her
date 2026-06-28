package com.linkyun.her;

final class SubtitleTypewriterText {
    private SubtitleTypewriterText() {}

    static String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    static boolean needsTypewriter(String text, int visibleCodePoints) {
        String clean = normalize(text);
        return visibleCodePoints > 0 && clean.codePointCount(0, clean.length()) > visibleCodePoints;
    }

    static String frame(String text, int frame, int visibleCodePoints) {
        String clean = normalize(text);
        if (clean.isEmpty() || visibleCodePoints <= 0) return "";
        int totalCodePoints = clean.codePointCount(0, clean.length());
        if (totalCodePoints <= visibleCodePoints) return clean;

        int cycle = totalCodePoints + visibleCodePoints;
        int normalizedFrame = frame % cycle;
        if (normalizedFrame < 0) normalizedFrame += cycle;
        int endCodePoint = Math.max(1, Math.min(totalCodePoints, normalizedFrame + 1));
        int startCodePoint = Math.max(0, endCodePoint - visibleCodePoints);
        int start = clean.offsetByCodePoints(0, startCodePoint);
        int end = clean.offsetByCodePoints(0, endCodePoint);
        return clean.substring(start, end);
    }
}
