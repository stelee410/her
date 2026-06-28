package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TextFallbackNavigationDecisionTest {
    @Test
    public void initializedVoiceSurfaceReturnsToVoice() {
        assertEquals(TextFallbackNavigationDecision.ReturnSurface.VOICE,
                TextFallbackNavigationDecision.inferReturnSurface(
                        true, false, true, false, false,
                        TextFallbackNavigationDecision.ReturnSurface.HOME));
    }

    @Test
    public void initializedHomeSurfaceReturnsToHome() {
        assertEquals(TextFallbackNavigationDecision.ReturnSurface.HOME,
                TextFallbackNavigationDecision.inferReturnSurface(
                        true, false, false, true, false,
                        TextFallbackNavigationDecision.ReturnSurface.VOICE));
    }

    @Test
    public void initializingReturnsToInitialization() {
        assertEquals(TextFallbackNavigationDecision.ReturnSurface.INITIALIZATION,
                TextFallbackNavigationDecision.inferReturnSurface(
                        false, true, true, false, false,
                        TextFallbackNavigationDecision.ReturnSurface.VOICE));
    }

    @Test
    public void rerenderKeepsExistingReturnSurface() {
        assertEquals(TextFallbackNavigationDecision.ReturnSurface.HOME,
                TextFallbackNavigationDecision.inferReturnSurface(
                        true, false, false, false, true,
                        TextFallbackNavigationDecision.ReturnSurface.HOME));
    }
}
