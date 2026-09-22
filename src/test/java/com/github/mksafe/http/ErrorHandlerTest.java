package com.github.mksafe.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorHandlerTest {

    private ErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new ErrorHandler("dummy-public");
    }

    @Test
    void testCreateErrorResponse_WithPathStartingWithApi_ReturnsJson() {
        Response response = errorHandler.createErrorResponse(Status.NOT_FOUND, Method.GET, "/api/users");

        assertEquals(Status.NOT_FOUND, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"error\": \"Not Found\"}\n", new String(response.getPayload()));
    }

    @Test
    void testCreateErrorResponse_WithApiBadRequest_ReturnsJson() {
        Response response = errorHandler.createErrorResponse(Status.BAD_REQUEST, Method.POST, "/api/echo");

        assertEquals(Status.BAD_REQUEST, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertEquals("{\"error\": \"Bad Request\"}\n", new String(response.getPayload()));
    }

    @Test
    void testCreateErrorResponse_WithCustomHtmlTemplate_ReturnsHtml() {
        Response response = errorHandler.createErrorResponse(Status.NOT_FOUND, Method.GET, "/not-found.html");

        assertEquals(Status.NOT_FOUND, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertTrue(new String(response.getPayload()).contains("404 Error"));
    }

    @Test
    void testCreateErrorResponse_WithoutPath_LoadsHtmlIfAvailable() {
        Response response = errorHandler.createErrorResponse(Status.UNKNOWN, Method.UNKNOWN);

        assertEquals(Status.UNKNOWN, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertTrue(new String(response.getPayload()).contains("501 Error"));
    }

    @Test
    void testCreateErrorResponse_WhenHtmlNotPresent_FallsBackToPlainText() {
        Response response = errorHandler.createErrorResponse(Status.BAD_REQUEST, Method.GET);

        assertEquals(Status.BAD_REQUEST, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertEquals("400 Bad Request", new String(response.getPayload()));
    }
}
