package com.linkyun.her;

final class AvatarPlaybackMode {
    static final String JESS = "jess";
    static final String ASSETS_FULLSCREEN = "assets_fullscreen";

    private AvatarPlaybackMode() {}

    static String normalize(String mode) {
        if (ASSETS_FULLSCREEN.equals(mode)) return ASSETS_FULLSCREEN;
        return JESS;
    }

    static boolean isAssetsFullscreen(String mode) {
        return ASSETS_FULLSCREEN.equals(normalize(mode));
    }

    static String next(String mode) {
        return isAssetsFullscreen(mode) ? JESS : ASSETS_FULLSCREEN;
    }

    static String settingsLabel(String mode) {
        return isAssetsFullscreen(mode) ? "assets 全屏 standby/talking" : "Jess 透明情感形象";
    }
}
