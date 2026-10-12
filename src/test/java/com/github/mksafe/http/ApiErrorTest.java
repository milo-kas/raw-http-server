package com.github.mksafe.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiErrorTest {

    @Test
    void testToJson_WithPlainMessage() {
        ApiError error = new ApiError("Not found");
        assertEquals("{\"error\": \"Not found\"}", error.toJson());
    }

    @Test
    void testToJson_WithNullMessage_ProducesEmptyError() {
        ApiError error = new ApiError(null);
        assertEquals("{\"error\": \"\"}", error.toJson());
    }

    @Test
    void testToJson_EscapesQuotesAndBackslashes() {
        ApiError error = new ApiError("He said \"hi\" at C:\\temp");
        assertEquals("{\"error\": \"He said \\\"hi\\\" at C:\\\\temp\"}", error.toJson());
    }

    @Test
    void testToJson_EscapesControlCharacters() {
        ApiError error = new ApiError("line1\nline2\ttab\rreturn");
        assertEquals("{\"error\": \"line1\\nline2\\ttab\\rreturn\"}", error.toJson());
    }

    @Test
    void testError_Accessor() {
        ApiError error = new ApiError("boom");
        assertEquals("boom", error.error());
    }
}
