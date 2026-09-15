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
}