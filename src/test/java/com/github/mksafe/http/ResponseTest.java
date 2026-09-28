package com.github.mksafe.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseTest {

    @Test
    void testRespond_WithInputStreamPayload_StreamsContentToOutputStream() throws IOException {
        byte[] data = "Streaming payload data".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(data);

        Response response = new Response(Status.OK, "text/plain", inputStream, data.length, Method.GET);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.contains("Content-Type: text/plain\r\n"));
        assertTrue(result.contains("Content-Length: " + data.length + "\r\n"));
        assertTrue(result.contains("Connection: close\r\n"));
        assertTrue(result.contains("X-Content-Type-Options: nosniff\r\n"));
        assertTrue(result.contains("X-Frame-Options: DENY\r\n"));
        assertTrue(result.endsWith("\r\n\r\nStreaming payload data"));
    }

    @Test
    void testRespond_WithHeadMethod_DoesNotWritePayloadBody() throws IOException {
        byte[] data = "Secret body that should not be transmitted".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(data);

        Response response = new Response(Status.OK, "text/plain", inputStream, data.length, Method.HEAD);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.contains("Content-Type: text/plain\r\n"));
        assertTrue(result.contains("Content-Length: " + data.length + "\r\n"));
        assertTrue(result.endsWith("\r\n\r\n"));
        assertFalse(result.contains("Secret body"));
    }

    @Test
    void testRespond_WithHeadMethod_ClosesUnderlyingStream() throws IOException {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream trackedStream = new InputStream() {
            private final InputStream delegate = new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8));

            @Override
            public int read() throws IOException {
                return delegate.read();
            }

            @Override
            public void close() throws IOException {
                closed.set(true);
                delegate.close();
            }
        };

        Response response = new Response(Status.OK, "text/plain", trackedStream, 4, Method.HEAD);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        assertTrue(closed.get(), "HEAD method must still close the underlying payload stream");
    }

    @Test
    void testRespond_WithByteArrayConstructor_StreamsCorrectly() throws IOException {
        byte[] data = "{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        Response response = new Response(Status.OK, "application/json", data, Method.GET);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.contains("Content-Type: application/json\r\n"));
        assertTrue(result.contains("Content-Length: " + data.length + "\r\n"));
        assertTrue(result.endsWith("\r\n\r\n{\"message\":\"hello\"}"));
    }

    @Test
    void testRespond_WithByteArray_HeadMethod_SuppressesBody() throws IOException {
        byte[] data = "{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        Response response = new Response(Status.OK, "application/json", data, Method.HEAD);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.contains("Content-Type: application/json\r\n"));
        assertTrue(result.contains("Content-Length: " + data.length + "\r\n"));
        assertTrue(result.endsWith("\r\n\r\n"));
        assertFalse(result.contains("hello"));
    }

    @Test
    void testGetPayload_WithInputStream_CanBeReadMultipleTimes() {
        byte[] data = "Repeated read test".getBytes(StandardCharsets.UTF_8);
        Response response = new Response(Status.OK, "text/plain", new ByteArrayInputStream(data), data.length, Method.GET);

        byte[] firstRead = response.getPayload();
        byte[] secondRead = response.getPayload();

        assertNotNull(firstRead);
        assertNotNull(secondRead);
        assertArrayEquals(data, firstRead);
        assertArrayEquals(data, secondRead);
    }

    @Test
    void testGetPayload_WhenPayloadIsNull_ReturnsNull() {
        Response response = new Response(Status.OK, "text/plain", null, 0, Method.GET);
        assertNull(response.getPayload());

        Response responseBytes = new Response(Status.OK, "text/plain", null, Method.GET);
        assertNull(responseBytes.getPayload());
    }

    @Test
    void testGetters() {
        byte[] data = "Test".getBytes(StandardCharsets.UTF_8);
        InputStream stream = new ByteArrayInputStream(data);
        Response response = new Response(Status.CREATED, "text/plain", stream, data.length, Method.POST);

        assertEquals(Status.CREATED, response.getStatus());
        assertEquals("text/plain", response.getContentType());
        assertEquals(data.length, response.getContentLength());
        assertEquals(Method.POST, response.getMethod());
        assertNotNull(response.getPayloadStream());
    }

    @Test
    void testRespond_WhenPayloadIsNull_WritesHeadersOnly() throws IOException {
        Response response = new Response(Status.OK, "text/plain", null, 0, Method.GET);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.endsWith("\r\n\r\n"));
    }

    @Test
    void testRespond_WithNegativeContentLength_OmitsContentLengthHeader() throws IOException {
        Response response = new Response(Status.OK, "text/plain", new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)), -1, Method.GET);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertFalse(result.contains("Content-Length:"));
    }

    @Test
    void testRespond_AfterCallingGetPayload_StreamsCachedPayload() throws IOException {
        byte[] data = "Cached payload streaming".getBytes(StandardCharsets.UTF_8);
        Response response = new Response(Status.OK, "text/plain", new ByteArrayInputStream(data), data.length, Method.GET);

        byte[] payloadBytes = response.getPayload();
        assertArrayEquals(data, payloadBytes);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.endsWith("\r\n\r\nCached payload streaming"));
    }

    @Test
    void testText_FactoryHelpers() throws IOException {
        Response r1 = Response.text("Sample text", Method.GET);
        assertEquals(Status.OK, r1.getStatus());
        assertEquals("text/plain", r1.getContentType());
        assertEquals("Sample text", new String(r1.getPayload(), StandardCharsets.UTF_8));
        assertEquals(Method.GET, r1.getMethod());
        assertEquals("Sample text".getBytes(StandardCharsets.UTF_8).length, r1.getContentLength());

        Response r2 = Response.text(Status.CREATED, "Created text", Method.POST);
        assertEquals(Status.CREATED, r2.getStatus());
        assertEquals("text/plain", r2.getContentType());
        assertEquals("Created text", new String(r2.getPayload(), StandardCharsets.UTF_8));
        assertEquals(Method.POST, r2.getMethod());

        Response r3 = Response.text(Status.BAD_REQUEST, "Bad Request", Method.HEAD);
        assertEquals(Status.BAD_REQUEST, r3.getStatus());
        assertEquals(Method.HEAD, r3.getMethod());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        r3.respond(out);
        String headOutput = out.toString(StandardCharsets.UTF_8);
        assertTrue(headOutput.startsWith("HTTP/1.1 400 Bad Request\r\n"));
        assertTrue(headOutput.endsWith("\r\n\r\n"));
        assertFalse(headOutput.contains("Bad Request\r\n\r\nBad Request"));
    }

    @Test
    void testJson_FactoryHelpers() throws IOException {
        Response r1 = Response.json("{\"message\":\"success\"}", Method.GET);
        assertEquals(Status.OK, r1.getStatus());
        assertEquals("application/json", r1.getContentType());
        assertEquals("{\"message\":\"success\"}", new String(r1.getPayload(), StandardCharsets.UTF_8));
        assertEquals(Method.GET, r1.getMethod());

        Response r2 = Response.json(Status.BAD_REQUEST, "{\"error\":\"bad\"}", Method.POST);
        assertEquals(Status.BAD_REQUEST, r2.getStatus());
        assertEquals("application/json", r2.getContentType());
        assertEquals(Method.POST, r2.getMethod());

        Response r3 = Response.json(Status.OK, "{\"key\":\"val\"}", Method.HEAD);
        assertEquals(Method.HEAD, r3.getMethod());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        r3.respond(out);
        String headOutput = out.toString(StandardCharsets.UTF_8);
        assertTrue(headOutput.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(headOutput.endsWith("\r\n\r\n"));
        assertFalse(headOutput.contains("{\"key\":\"val\"}"));
    }

    @Test
    void testHtml_FactoryHelpers() throws IOException {
        Response r1 = Response.html("<h1>Title</h1>", Method.GET);
        assertEquals(Status.OK, r1.getStatus());
        assertEquals("text/html", r1.getContentType());
        assertEquals("<h1>Title</h1>", new String(r1.getPayload(), StandardCharsets.UTF_8));
        assertEquals(Method.GET, r1.getMethod());

        Response r2 = Response.html(Status.NOT_FOUND, "<h1>404</h1>", Method.GET);
        assertEquals(Status.NOT_FOUND, r2.getStatus());
        assertEquals("text/html", r2.getContentType());
        assertEquals("<h1>404</h1>", new String(r2.getPayload(), StandardCharsets.UTF_8));

        Response r3 = Response.html(Status.OK, "<h1>Title</h1>", Method.HEAD);
        assertEquals(Method.HEAD, r3.getMethod());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        r3.respond(out);
        String headOutput = out.toString(StandardCharsets.UTF_8);
        assertTrue(headOutput.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(headOutput.endsWith("\r\n\r\n"));
        assertFalse(headOutput.contains("<h1>Title</h1>"));
    }

    @Test
    void testUtf8_CharsetEncodingForPayload() {
        String utf8Content = "Hello, 世界 🌍! Übergröße";
        Response response = Response.text(utf8Content, Method.GET);
        byte[] expectedBytes = utf8Content.getBytes(StandardCharsets.UTF_8);

        assertEquals(expectedBytes.length, response.getContentLength());
        assertArrayEquals(expectedBytes, response.getPayload());
    }

    @Test
    void testNullPayload_FactoryHelpers() {
        Response r1 = Response.text(null, Method.GET);
        assertEquals(0, r1.getContentLength());
        assertNull(r1.getPayload());

        Response r2 = Response.json(null, Method.POST);
        assertEquals(0, r2.getContentLength());
        assertNull(r2.getPayload());

        Response r3 = Response.html(null, Method.GET);
        assertEquals(0, r3.getContentLength());
        assertNull(r3.getPayload());
    }

    @Test
    void testContentType_JsonAndPlain() {
        assertEquals("application/json", ContentType.JSON.getContentType());
        assertEquals("text/plain", ContentType.PLAIN.getContentType());
        assertEquals(ContentType.JSON, ContentType.fromExtension("json"));
        assertEquals(ContentType.PLAIN, ContentType.fromExtension("txt"));
    }
}
