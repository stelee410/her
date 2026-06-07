package com.linkyun.her;

final class HeadsetSettingsLabel {
    private HeadsetSettingsLabel() {
    }

    static String build(boolean demoMode, boolean hasBoundHeadset, String boundLabel,
            boolean boundConnected) {
        if (demoMode) return "Skipped in demo mode";
        if (!hasBoundHeadset) return "Not bound";
        String label = boundLabel == null ? "" : boundLabel.trim();
        if (label.isEmpty()) label = "Bound";
        return boundConnected ? label : label + " · offline";
    }
}
