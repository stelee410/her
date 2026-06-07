package com.linkyun.her;

final class PermissionResultDecision {
    enum Action {
        IGNORE,
        RECORD_GRANTED,
        RECORD_DENIED,
        LOCATION_RESULT
    }

    final Action action;
    final boolean granted;

    private PermissionResultDecision(Action action, boolean granted) {
        this.action = action;
        this.granted = granted;
    }

    static PermissionResultDecision decide(int requestCode, int[] grants,
            int recordRequestCode, int locationRequestCode, int grantedValue) {
        if (requestCode == recordRequestCode) {
            boolean granted = grants != null && grants.length > 0 && grants[0] == grantedValue;
            return new PermissionResultDecision(
                    granted ? Action.RECORD_GRANTED : Action.RECORD_DENIED,
                    granted);
        }
        if (requestCode == locationRequestCode) {
            return new PermissionResultDecision(Action.LOCATION_RESULT,
                    hasAnyGrant(grants, grantedValue));
        }
        return new PermissionResultDecision(Action.IGNORE, false);
    }

    private static boolean hasAnyGrant(int[] grants, int grantedValue) {
        if (grants == null) return false;
        for (int grant : grants) {
            if (grant == grantedValue) return true;
        }
        return false;
    }
}
