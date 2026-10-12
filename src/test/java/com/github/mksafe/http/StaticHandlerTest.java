package com.github.mksafe.http;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticHandlerTest {

    private StaticHandler staticHandler;

    @BeforeEach
    void setUp() {
        staticHandler = new StaticHandler("dummy-public");
    }

    @Test
    void handleStaticResource_ValidHtmlFile_Returns200AndHtmlContentType() {
        Request request = new Request(Method.GET, "/index.html", null);
        Response response = staticHandler.handleStaticResource(request);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertNotNull(response.getPayload());
        assertTrue(new String(response.getPayload()).contains("HELLO WORLD!"));
    }

    @Test
    void handleStaticResource_RootPath_ResolvesToIndexHtml() {
        Request request = new Request(Method.GET, "/", null);
        Response response = staticHandler.handleStaticResource(request);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertTrue(new String(response.getPayload()).contains("HELLO WORLD!"));
    }

    @Test
    void handleStaticResource_BinaryFile_ReturnsCorrectContentType() {
        Request request = new Request(Method.GET, "/favicon.ico", null);
        Response response = staticHandler.handleStaticResource(request);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("image/x-icon", response.getContentType());
        assertNotNull(response.getPayload());
    }

    @Test
    void handleStaticResource_HeadMethod_Returns200WithContentType() {
        Request request = new Request(Method.HEAD, "/index.html", null);
        Response response = staticHandler.handleStaticResource(request);

        assertEquals(Status.OK, response.getStatus());
        assertEquals("text/html", response.getContentType());
    }

    @Test
    void handleStaticResource_PathTraversal_Returns404() {
        Request request = new Request(Method.GET, "/../secret.txt", null);
        Response response = staticHandler.handleStaticResource(request);

        assertEquals(Status.NOT_FOUND, response.getStatus());
    }

    @Test
    void handleStaticResource_NonExistentFile_Returns404WithCustomErrorHtml() {
        Request request = new Request(Method.GET, "/non-existent-page.html", null);
        Response response = staticHandler.handleStaticResource(request);

        assertEquals(Status.NOT_FOUND, response.getStatus());
        assertEquals("text/html", response.getContentType());
        assertTrue(new String(response.getPayload()).contains("404 Error"));
    }
}

