package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class WeatherRequestCoordinatorTest {
    @Test
    public void missingPermissionStoresPendingAndRequestsPermission() {
        Host host = new Host();
        host.permission = false;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", true, 7);

        assertTrue(coordinator.hasPendingRequest());
        assertEquals("permission", host.events.get(0));
    }

    @Test
    public void deniedPermissionFailsPendingRequestAndClearsIt() {
        Host host = new Host();
        host.permission = false;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", true, 7);
        coordinator.onLocationPermissionResult(false);

        assertFalse(coordinator.hasPendingRequest());
        assertEquals("fail:天气:没有定位权限，请告诉我城市名。:true:7", host.events.get(1));
    }

    @Test
    public void grantedPermissionContinuesPendingRequest() {
        Host host = new Host();
        host.permission = false;
        host.managerAvailable = true;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", false, 3);
        host.permission = true;
        coordinator.onLocationPermissionResult(true);

        assertFalse(coordinator.hasPendingRequest());
        assertEquals("single", host.events.get(1));
    }

    @Test
    public void latestPendingPermissionRequestWinsWhenPermissionIsGranted() {
        Host host = new Host();
        host.permission = false;
        host.managerAvailable = true;
        host.lastLocation = new Location("test");
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("旧天气", true, 1);
        coordinator.requestCurrentLocation("新天气", false, 2);
        host.permission = true;
        coordinator.onLocationPermissionResult(true);

        assertFalse(coordinator.hasPendingRequest());
        assertEquals("query:新天气:false:2", host.events.get(2));
    }

    @Test
    public void latestPendingPermissionRequestWinsWhenPermissionIsDenied() {
        Host host = new Host();
        host.permission = false;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("旧天气", true, 1);
        coordinator.requestCurrentLocation("新天气", false, 2);
        coordinator.onLocationPermissionResult(false);

        assertFalse(coordinator.hasPendingRequest());
        assertEquals("fail:新天气:没有定位权限，请告诉我城市名。:false:2", host.events.get(2));
    }

    @Test
    public void clearedPendingPermissionRequestIgnoresLaterPermissionResult() {
        Host host = new Host();
        host.permission = false;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", true, 7);
        coordinator.clearPending();
        coordinator.onLocationPermissionResult(true);
        coordinator.onLocationPermissionResult(false);

        assertFalse(coordinator.hasPendingRequest());
        assertEquals(1, host.events.size());
        assertEquals("permission", host.events.get(0));
    }

    @Test
    public void noLocationManagerFailsImmediately() {
        Host host = new Host();
        host.permission = true;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", false, 3);

        assertEquals("fail:天气:无法读取当前位置:false:3", host.events.get(0));
    }

    @Test
    public void lastLocationIsUsedBeforeSingleLocationRequest() {
        Host host = new Host();
        host.permission = true;
        host.managerAvailable = true;
        host.lastLocation = new Location("test");
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", true, 4);

        assertEquals("query:天气:true:4", host.events.get(0));
    }

    @Test
    public void clearedSingleLocationRequestIgnoresLateSuccessAndError() {
        Host host = new Host();
        host.permission = true;
        host.managerAvailable = true;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("天气", true, 8);
        coordinator.clearPending();
        host.successCallbacks.get(0).onLocation(new Location("stale"));
        host.errorCallbacks.get(0).onError("late error");

        assertEquals(1, host.events.size());
        assertEquals("single", host.events.get(0));
    }

    @Test
    public void newCurrentLocationRequestInvalidatesPreviousSingleLocationCallbacks() {
        Host host = new Host();
        host.permission = true;
        host.managerAvailable = true;
        WeatherRequestCoordinator coordinator = new WeatherRequestCoordinator(host);

        coordinator.requestCurrentLocation("旧天气", true, 1);
        coordinator.requestCurrentLocation("新天气", false, 2);
        host.successCallbacks.get(0).onLocation(new Location("old"));
        host.errorCallbacks.get(0).onError("old error");
        host.successCallbacks.get(1).onLocation(new Location("new"));

        assertEquals("single", host.events.get(0));
        assertEquals("single", host.events.get(1));
        assertEquals("query:新天气:false:2", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    private static final class Host implements WeatherRequestCoordinator.Host {
        final List<String> events = new ArrayList<>();
        final List<WeatherSkill.LocationCallback> successCallbacks = new ArrayList<>();
        final List<WeatherSkill.ErrorCallback> errorCallbacks = new ArrayList<>();
        boolean permission;
        boolean managerAvailable;
        Location lastLocation;

        @Override public boolean hasLocationPermission() {
            return permission;
        }

        @Override public void requestLocationPermission() {
            events.add("permission");
        }

        @Override public boolean hasLocationManager() {
            return managerAvailable;
        }

        @Override public Location bestLastLocation() {
            return lastLocation;
        }

        @Override public void requestSingleLocation(WeatherSkill.LocationCallback success, WeatherSkill.ErrorCallback error) {
            events.add("single");
            successCallbacks.add(success);
            errorCallbacks.add(error);
        }

        @Override public void queryLocation(String question, Location location, boolean realtimeMode, int token) {
            events.add("query:" + question + ":" + realtimeMode + ":" + token);
        }

        @Override public void failWeather(String question, String message, boolean realtimeMode, int token) {
            events.add("fail:" + question + ":" + message + ":" + realtimeMode + ":" + token);
        }

        @Override public void logWeatherRequest(String message) {
            events.add("log:" + message);
        }
    }
}
