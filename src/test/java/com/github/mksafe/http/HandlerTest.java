package com.github.mksafe.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandlerTest {

    private Handler handler;

    @BeforeEach
    void setUp() {
        // Runs before every single test to give a fresh Handler
        handler = new Handler("dummy-public");
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

    // Test happy paths for static files
    @Test
    void testHandleGet_ValidStaticFile_Returns200_AndHtml() {
        Request validGet = new Request(Method.GET, "/index.html", null);
        Response response = handler.handleRequest(validGet);

        assertEquals(Status.OK, response.getStatus(), "Should return 200 OK");
        assertEquals("text/html", response.getContentType(), "Should parse .html extension correctly");

        String responseBody = new String(response.getPayload());
        assertTrue(responseBody.contains("HELLO WORLD!"), "Should read the dummy index.html file content");
    }

    @Test
    void testHandleGet_RootPath_DefaultsToIndexHtml() {
        Request rootGet = new Request(Method.GET, "/", null);
        Response response = handler.handleRequest(rootGet);

        assertEquals(Status.OK, response.getStatus(), "Should return 200 OK for root path");
        assertEquals("text/html", response.getContentType(), "Should resolve to dummy-public/index.html");
    }

    @Test
    void testHandleGet_MissingWebRoute_LoadsHtmlErrorPage() {
        Request missingWeb = new Request(Method.GET, "/does-not-exist.html", null);
        Response response = handler.handleRequest(missingWeb);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Should return 404 Not Found");
        assertEquals("text/html", response.getContentType(), "Should load the custom dummy 404.html page");

        String responseBody = new String(response.getPayload());
        assertTrue(responseBody.contains("404 Error"), "Should contain the HTML from dummy-public/error/404.html");
    }
}