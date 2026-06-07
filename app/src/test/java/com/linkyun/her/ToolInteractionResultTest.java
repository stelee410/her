package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ToolInteractionResultTest {
    @Test
    public void successNormalizesNullableTextFields() {
        Object payload = new Object();

        ToolInteractionResult<Object> result =
                ToolInteractionResult.success(null, null, null, null, payload);

        assertTrue(result.success);
        assertEquals("", result.tool);
        assertEquals("", result.question);
        assertEquals("", result.fact);
        assertEquals("", result.answer);
        assertEquals("", result.errorMessage);
        assertEquals(payload, result.payload);
    }

    @Test
    public void failureNormalizesNullableTextFieldsAndHasNoPayload() {
        ToolInteractionResult<Object> result =
                ToolInteractionResult.failure(null, null, null, null, null);

        assertFalse(result.success);
        assertEquals("", result.tool);
        assertEquals("", result.question);
        assertEquals("", result.fact);
        assertEquals("", result.answer);
        assertEquals("", result.errorMessage);
        assertNull(result.payload);
    }
}
