package com.linkyun.her;

final class TextFallbackNavigationDecision {
    private TextFallbackNavigationDecision() {
    }

    enum ReturnSurface {
        HOME,
        VOICE,
        INITIALIZATION
    }

    static ReturnSurface inferReturnSurface(boolean initialized, boolean initializing,
            boolean voiceInputSurfaceActive, boolean homeSurfaceActive, boolean alreadyTextMode,
            ReturnSurface currentReturnSurface) {
        if (alreadyTextMode && currentReturnSurface != null) return currentReturnSurface;
        if (!initialized || initializing) return ReturnSurface.INITIALIZATION;
        if (voiceInputSurfaceActive) return ReturnSurface.VOICE;
        if (homeSurfaceActive) return ReturnSurface.HOME;
        return ReturnSurface.HOME;
    }
}
