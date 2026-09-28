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
        assertTrue(!result.contains("Secret body"));
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
        Response response = new Response(Status.OK, "text/plain", (InputStream) null, 0, Method.GET);
        assertNull(response.getPayload());
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
        Response response = new Response(Status.OK, "text/plain", (InputStream) null, 0, Method.GET);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(result.startsWith("HTTP/1.1 200 OK\r\n"));
        assertTrue(result.endsWith("\r\n\r\n"));
    }

    @Test
    void testRespond_WithNegativeContentLength_OmitsContentLengthHeader() throws IOException {
        Response response = new Response(Status.OK, "text/plain", new ByteArrayInputStream("data".getBytes()), -1, Method.GET);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        response.respond(outputStream);

        String result = outputStream.toString(StandardCharsets.UTF_8);
        assertTrue(!result.contains("Content-Length:"));
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
}
