package com.github.mksafe.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiErrorTest {

    @Test
    void toJson_WithPlainMessage() {
        ApiError error = new ApiError("Not found");
        assertEquals("{\"error\": \"Not found\"}", error.toJson());
    }

    @Test
    void toJson_WithNullMessage_ProducesEmptyError() {
        ApiError error = new ApiError(null);
        assertEquals("{\"error\": \"\"}", error.toJson());
    }

    @Test
    void toJson_EscapesQuotesAndBackslashes() {
        ApiError error = new ApiError("He said \"hi\" at C:\\temp");
        assertEquals("{\"error\": \"He said \\\"hi\\\" at C:\\\\temp\"}", error.toJson());
    }

    @Test
    void toJson_EscapesControlCharacters() {
        ApiError error = new ApiError("line1\nline2\ttab\rreturn");
        assertEquals("{\"error\": \"line1\\nline2\\ttab\\rreturn\"}", error.toJson());
    }

    @Test
    void error_Accessor() {
        ApiError error = new ApiError("boom");
        assertEquals("boom", error.error());
    }
}
