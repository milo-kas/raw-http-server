package com.github.mksafe.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouterTest {

    private Router router;

    @BeforeEach
    void setUp() {
        // Runs before every single test to give a fresh Router with test routes
        router = new Router("dummy-public");
        router.post("/api/echo", request -> {
            String payload = request.getPayload();
            if (payload == null || payload.isBlank()) {
                return new Response(Status.BAD_REQUEST, "application/json", "{\"error\": \"Bad Request\"}\n".getBytes(), request.getMethod());
            }
            return new Response(Status.CREATED, "text/plain", payload.getBytes(), request.getMethod());
        });
    }

    @Test
    void testHandleRequest_WhenFileDoesNotExist_Returns404() {
        // Create a GET request for a bad path
        Request badPath = new Request(Method.GET, "/bad-path", null);
        Response response = router.handleRequest(badPath);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Should return a 404 Not Found");
    }

    @Test
    void testHandleRequest_WhenMethodIsUnknown_Returns501() {
        // Create a request with an unsupported HTTP method
        Request fakeRequest = new Request(Method.UNKNOWN, "/index.html", null);
        Response response = router.handleRequest(fakeRequest);

        assertEquals(Status.UNKNOWN, response.getStatus(), "Should return a 501 Unknown error");
    }

    @Test
    void testHandleRequest_WhenPathTraversalAttempted_Returns404() {
        // Try to traverse above the public root
        Request fakeRequest = new Request(Method.GET, "/../secret-passwords.txt", null);
        Response response = router.handleRequest(fakeRequest);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Path traversal must be blocked with a 404");
    }

    // API TESTS
    @Test
    void testHandlePost_ApiEcho_WithValidPayload_Returns201() {
        Request validPost = new Request(Method.POST, "/api/echo", "Hello World");
        Response response = router.handleRequest(validPost);

        assertEquals(Status.CREATED, response.getStatus(), "Should return 201 Created");

        String responseBody = new String(response.getPayload());
        assertEquals("Hello World", responseBody, "Should echo the payload exactly");
    }

    @Test
    void testHandlePost_ApiEcho_WithBlankPayload_Returns400_AndJson() {
        Request badPost = new Request(Method.POST, "/api/echo", "   ");
        Response response = router.handleRequest(badPost);

        assertEquals(Status.BAD_REQUEST, response.getStatus(), "Should return 400 Bad Request");
        assertEquals("application/json", response.getContentType(), "API errors must return JSON");

        String responseBody = new String(response.getPayload());
        assertEquals("{\"error\": \"Bad Request\"}\n", responseBody, "Should format error as JSON");
    }

    @Test
    void testHandleGet_MissingApiRoute_Returns404_AndJson() {
        Request missingApi = new Request(Method.GET, "/api/nonexistent", null);
        Response response = router.handleRequest(missingApi);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Should return 404 Not Found");
        assertEquals("application/json", response.getContentType(), "Missing API routes must return JSON");

        String responseBody = new String(response.getPayload());
        assertEquals("{\"error\": \"Not Found\"}\n", responseBody, "Should format error as JSON");
    }

    // Test happy paths for static files
    @Test
    void testHandleGet_ValidStaticFile_Returns200_AndHtml() {
        Request validGet = new Request(Method.GET, "/index.html", null);
        Response response = router.handleRequest(validGet);

        assertEquals(Status.OK, response.getStatus(), "Should return 200 OK");
        assertEquals("text/html", response.getContentType(), "Should parse .html extension correctly");

        String responseBody = new String(response.getPayload());
        assertTrue(responseBody.contains("HELLO WORLD!"), "Should read the dummy index.html file content");
    }

    @Test
    void testHandleGet_RootPath_DefaultsToIndexHtml() {
        Request rootGet = new Request(Method.GET, "/", null);
        Response response = router.handleRequest(rootGet);

        assertEquals(Status.OK, response.getStatus(), "Should return 200 OK for root path");
        assertEquals("text/html", response.getContentType(), "Should resolve to dummy-public/index.html");
    }

    @Test
    void testHandleGet_MissingWebRoute_LoadsHtmlErrorPage() {
        Request missingWeb = new Request(Method.GET, "/does-not-exist.html", null);
        Response response = router.handleRequest(missingWeb);

        assertEquals(Status.NOT_FOUND, response.getStatus(), "Should return 404 Not Found");
        assertEquals("text/html", response.getContentType(), "Should load the custom dummy 404.html page");

        String responseBody = new String(response.getPayload());
        assertTrue(responseBody.contains("404 Error"), "Should contain the HTML from dummy-public/error/404.html");
    }

    @Test
    void testHandleGet_DynamicCustomRoute_ReturnsOk() {
        router.get("/api/greet", req -> new Response(Status.OK, "text/plain", "Greetings!".getBytes(), req.getMethod()));

        Request request = new Request(Method.GET, "/api/greet", null);
        Response response = router.handleRequest(request);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("Greetings!", new String(response.getPayload()));
    }

    @Test
    void testHandleRequest_PathNormalisation_MatchesRoutesWithoutLeadingSlash() {
        router.get("custom/path", req -> new Response(Status.OK, "text/plain", "Matched".getBytes(), req.getMethod()));

        Request requestWithSlash = new Request(Method.GET, "/custom/path", null);
        Response response = router.handleRequest(requestWithSlash);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("Matched", new String(response.getPayload()));
    }

    @Test
    void testHandleRequest_HandlerThrowsException_Returns500() {
        router.get("/api/failing", req -> {
            throw new RuntimeException("Simulated crash");
        });

        Request request = new Request(Method.GET, "/api/failing", null);
        Response response = router.handleRequest(request);

        assertEquals(Status.INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(new String(response.getPayload()).contains("Internal Server Error"));
    }
}
