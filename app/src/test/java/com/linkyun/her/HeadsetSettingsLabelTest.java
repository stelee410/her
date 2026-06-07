package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class HeadsetSettingsLabelTest {
    @Test
    public void demoModeSkipsHeadsetBinding() {
        assertEquals("Skipped in demo mode",
                HeadsetSettingsLabel.build(true, false, "", false));
    }

    @Test
    public void unboundShowsNotBound() {
        assertEquals("Not bound",
                HeadsetSettingsLabel.build(false, false, "AirPods", true));
    }

    @Test
    public void connectedBoundHeadsetShowsLabelOrFallback() {
        assertEquals("AirPods",
                HeadsetSettingsLabel.build(false, true, " AirPods ", true));
        assertEquals("Bound",
                HeadsetSettingsLabel.build(false, true, "", true));
    }

    @Test
    public void offlineBoundHeadsetShowsOfflineSuffix() {
        assertEquals("AirPods · offline",
                HeadsetSettingsLabel.build(false, true, "AirPods", false));
        assertEquals("Bound · offline",
                HeadsetSettingsLabel.build(false, true, null, false));
    }
}
