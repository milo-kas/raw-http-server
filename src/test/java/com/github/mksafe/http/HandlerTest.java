package com.github.mksafe.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandlerTest {

    private Handler handler;

    @BeforeEach
    void setUp() {
        // Runs before every single test to give a fresh Handler
        handler = new Handler("public");
    }

    @Test
    void testHandleRequest_WhenFileDoesNotExist_Returns404() {
        // Create a GET request for a bad path
        Request badPath = new Request(Method.GET, "/bad-path", null);
        Response response = handler.handleRequest(badPath);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Should return a 404 Not Found");
    }

    @Test
    void testHandleRequest_WhenMethodIsUnknown_Returns501() throws IOException {
        // Create a request with an unsupported HTTP method
        Request fakeRequest = new Request(Method.UNKNOWN, "/index.html", null);
        Response response = handler.handleRequest(fakeRequest);

        assertEquals(Status.UNKNOWN, response.getStatus(), "Should return a 501 Unknown error");
    }

    @Test
    void testHandleRequest_WhenPathTraversalAttempted_Returns404() {
        // Try to traverse above the public root
        Request fakeRequest = new Request(Method.GET, "/../secret-passwords.txt", null);
        Response response = handler.handleRequest(fakeRequest);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Path traversal must be blocked with a 404");
    }

    // API TESTS
    @Test
    void testHandlePost_ApiEcho_WithValidPayload_Returns201() {
        Request validPost = new Request(Method.POST, "/api/echo", "Hello World");
        Response response = handler.handleRequest(validPost);

        assertEquals(Status.CREATED, response.getStatus(), "Should return 201 Created");

        String responseBody = new String(response.getPayload());
        assertEquals("Hello World", responseBody, "Should echo the payload exactly");
    }

    @Test
    void testHandlePost_ApiEcho_WithBlankPayload_Returns400_AndJson() {
        Request badPost = new Request(Method.POST, "/api/echo", "   ");
        Response response = handler.handleRequest(badPost);

        assertEquals(Status.BAD_REQUEST, response.getStatus(), "Should return 400 Bad Request");
        assertEquals("application/json", response.getContentType(), "API errors must return JSON");

        String responseBody = new String(response.getPayload());
        assertEquals("{\"error\": \"Bad Request\"}\n", responseBody, "Should format error as JSON");
    }

    @Test
    void testHandleGet_MissingApiRoute_Returns404_AndJson() {
        Request missingApi = new Request(Method.GET, "/api/nonexistent", null);
        Response response = handler.handleRequest(missingApi);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Should return 404 Not Found");
        assertEquals("application/json", response.getContentType(), "Missing API routes must return JSON");

        String responseBody = new String(response.getPayload());
        assertEquals("{\"error\": \"Not Found\"}\n", responseBody, "Should format error as JSON");
    }
}