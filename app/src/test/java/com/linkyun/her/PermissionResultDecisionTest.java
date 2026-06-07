package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PermissionResultDecisionTest {
    private static final int REQ_AUDIO = 71;
    private static final int REQ_LOCATION = 73;
    private static final int GRANTED = 0;
    private static final int DENIED = -1;

    @Test
    public void recordPermissionUsesFirstGrant() {
        PermissionResultDecision granted = PermissionResultDecision.decide(
                REQ_AUDIO, new int[] {GRANTED}, REQ_AUDIO, REQ_LOCATION, GRANTED);
        PermissionResultDecision denied = PermissionResultDecision.decide(
                REQ_AUDIO, new int[] {DENIED, GRANTED}, REQ_AUDIO, REQ_LOCATION, GRANTED);

        assertEquals(PermissionResultDecision.Action.RECORD_GRANTED, granted.action);
        assertTrue(granted.granted);
        assertEquals(PermissionResultDecision.Action.RECORD_DENIED, denied.action);
        assertFalse(denied.granted);
    }

    @Test
    public void recordPermissionDeniesEmptyOrMissingGrantArray() {
        assertEquals(PermissionResultDecision.Action.RECORD_DENIED,
                PermissionResultDecision.decide(
                        REQ_AUDIO, new int[0], REQ_AUDIO, REQ_LOCATION, GRANTED).action);
        assertEquals(PermissionResultDecision.Action.RECORD_DENIED,
                PermissionResultDecision.decide(
                        REQ_AUDIO, null, REQ_AUDIO, REQ_LOCATION, GRANTED).action);
    }

    @Test
    public void locationPermissionUsesAnyGrant() {
        PermissionResultDecision decision = PermissionResultDecision.decide(
                REQ_LOCATION, new int[] {DENIED, GRANTED}, REQ_AUDIO, REQ_LOCATION, GRANTED);

        assertEquals(PermissionResultDecision.Action.LOCATION_RESULT, decision.action);
        assertTrue(decision.granted);
    }

    @Test
    public void unknownPermissionRequestIsIgnored() {
        PermissionResultDecision decision = PermissionResultDecision.decide(
                999, new int[] {GRANTED}, REQ_AUDIO, REQ_LOCATION, GRANTED);

        assertEquals(PermissionResultDecision.Action.IGNORE, decision.action);
        assertFalse(decision.granted);
    }
}
