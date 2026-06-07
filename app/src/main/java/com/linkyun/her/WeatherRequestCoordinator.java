package com.linkyun.her;

import android.location.Location;

final class WeatherRequestCoordinator {
    interface Host {
        boolean hasLocationPermission();
        void requestLocationPermission();
        boolean hasLocationManager();
        Location bestLastLocation();
        void requestSingleLocation(WeatherSkill.LocationCallback success, WeatherSkill.ErrorCallback error);
        void queryLocation(String question, Location location, boolean realtimeMode, int token);
        void failWeather(String question, String message, boolean realtimeMode, int token);
        void logWeatherRequest(String message);
    }

    private final Host host;
    private String pendingQuestion;
    private boolean pendingRealtimeMode;
    private int pendingToken;
    private int pendingRequestId;
    private int requestSeq;

    WeatherRequestCoordinator(Host host) {
        this.host = host;
    }

    boolean hasPendingRequest() {
        return pendingQuestion != null;
    }

    void requestCurrentLocation(String question, boolean realtimeMode, int token) {
        int requestId = ++requestSeq;
        if (!host.hasLocationPermission()) {
            pendingQuestion = question;
            pendingRealtimeMode = realtimeMode;
            pendingToken = token;
            pendingRequestId = requestId;
            host.requestLocationPermission();
            return;
        }
        runWithPermission(question, realtimeMode, token, requestId);
    }

    void onLocationPermissionResult(boolean granted) {
        String question = pendingQuestion;
        boolean realtimeMode = pendingRealtimeMode;
        int token = pendingToken;
        int requestId = pendingRequestId;
        clearPendingFields();
        if (question == null) return;
        if (granted) {
            runWithPermission(question, realtimeMode, token, requestId);
        } else {
            failIfCurrent(requestId, question, "没有定位权限，请告诉我城市名。", realtimeMode, token);
        }
    }

    void clearPending() {
        requestSeq++;
        clearPendingFields();
    }

    private void clearPendingFields() {
        pendingQuestion = null;
        pendingRealtimeMode = false;
        pendingToken = 0;
        pendingRequestId = 0;
    }

    private void runWithPermission(String question, boolean realtimeMode, int token, int requestId) {
        if (!isCurrent(requestId)) return;
        if (!host.hasLocationManager()) {
            failIfCurrent(requestId, question, "无法读取当前位置", realtimeMode, token);
            return;
        }
        try {
            Location last = host.bestLastLocation();
            if (!isCurrent(requestId)) return;
            if (last != null) {
                queryIfCurrent(requestId, question, last, realtimeMode, token);
                return;
            }
            host.requestSingleLocation(
                    location -> queryIfCurrent(requestId, question, location, realtimeMode, token),
                    message -> failIfCurrent(requestId, question, message, realtimeMode, token));
        } catch (SecurityException error) {
            failIfCurrent(requestId, question, "没有定位权限", realtimeMode, token);
        }
    }

    private boolean isCurrent(int requestId) {
        return requestId == requestSeq;
    }

    private void queryIfCurrent(int requestId, String question, Location location,
            boolean realtimeMode, int token) {
        if (isCurrent(requestId)) host.queryLocation(question, location, realtimeMode, token);
    }

    private void failIfCurrent(int requestId, String question, String message,
            boolean realtimeMode, int token) {
        if (isCurrent(requestId)) host.failWeather(question, message, realtimeMode, token);
    }
}
