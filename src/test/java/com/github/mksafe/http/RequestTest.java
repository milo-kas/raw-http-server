package com.github.mksafe.http;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void parseRequest_ExtractsPathQueryAndPayload() throws IOException {
        String raw = "POST /api/users?page=2&tag=c%23 HTTP/1.1\r\n" +
                     "Host: example.test:8080\r\n" +
                     "Content-Length: 4\r\n\r\n" +
                     "data";
        Request req = parse(raw);

        assertEquals(Method.POST, req.method());
        assertEquals("/api/users", req.path());
        assertEquals("example.test:8080", req.header("host"));
        assertEquals("2", req.queryParam("page"));
        assertEquals("c#", req.queryParam("tag"));
        assertArrayEquals("data".getBytes(StandardCharsets.ISO_8859_1), req.payload());
    }

    @Test
    void parseRequest_PreservesBinaryPayload() throws IOException {
        byte[] body = {0x00, (byte) 0xFF, (byte) 0xE4, (byte) 0xBD, (byte) 0xA0, 0x0A, (byte) 0x80};

        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        raw.write(("POST /upload HTTP/1.1\r\nContent-Length: " + body.length + "\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII));
        raw.write(body);

        Request req = parse(raw.toByteArray());

        assertArrayEquals(body, req.payload());
    }

    @Test
    void parseRequest_RejectsInvalidHeaders() {
        assertThrows(IOException.class, () -> parse("GET / HTTP/1.1\r\nBadHeader\r\n\r\n"));
        assertThrows(IOException.class, () -> parse(
            "POST / HTTP/1.1\r\nContent-Length: 4\r\ncontent-length: 4\r\n\r\ndata"));
        assertThrows(IOException.class, () -> parse("POST / HTTP/1.1\r\nContent-Length: nope\r\n\r\n"));
        assertThrows(IOException.class, () -> parse("POST / HTTP/1.1\r\nContent-Length: -1\r\n\r\n"));
    }

    @Test
    void queryParam_HandlesFlagsAndMissingValues() {
        Request req = new Request(Method.GET, "/test", "active&debug=&empty", null);

        assertEquals("", req.queryParam("active"));
        assertEquals("", req.queryParam("debug"));
        assertNull(req.queryParam("absent"));
    }

    @Test
    void queryParam_HandlesNullBlankAndMalformedQueries() {
        Request nullQuery = new Request(Method.GET, "/test");
        assertNull(nullQuery.queryParam("key"));
        assertTrue(nullQuery.queryParams().isEmpty());

        Request malformed = new Request(Method.GET, "/test", "bad=%ZZ", null);
        assertEquals("%ZZ", malformed.queryParam("bad"));
    }

    @Test
    void parseRequest_ThrowsOnInvalidOrEmptyInput() {
        assertThrows(IOException.class, () -> parse(""));
        assertThrows(IOException.class, () -> parse("BAD\r\n\r\n"));
    }

    @Test
    void parseRequest_ReportsTruncatedPayloadWhenRead() throws IOException {
        String raw = "POST /upload HTTP/1.1\r\nContent-Length: 10\r\n\r\nshort";
        Request req = parse(raw);

        IOException error = assertThrows(IOException.class, () -> req.body().bytes(10));
        assertTrue(error.getMessage().contains("Unexpected end of payload"));
    }

    @Test
    void parseRequest_DoesNotReadPayloadDuringParsing() throws IOException {
        byte[] headers = ("POST /upload HTTP/1.1\r\nContent-Length: 4\r\n\r\n")
                .getBytes(StandardCharsets.US_ASCII);
        InputStream input = new InputStream() {
            private int position;

            @Override
            public int read() {
                if (position == headers.length) {
                    throw new AssertionError("Body read during parsing");
                }
                return headers[position++] & 0xFF;
            }

            @Override
            public int read(byte[] buffer, int offset, int length) {
                if (position == headers.length) {
                    throw new AssertionError("Body read during parsing");
                }
                int count = Math.min(length, headers.length - position);
                System.arraycopy(headers, position, buffer, offset, count);
                position += count;
                return count;
            }
        };

        Request req = Request.parseRequest(input);

        assertEquals(4, req.body().length());
    }

    @Test
    void parseRequest_UsesOptionalPayloadLimit() throws IOException {
        String raw = "POST /upload HTTP/1.1\r\nContent-Length: " + (1024 * 1024 + 1) + "\r\n\r\n";

        assertThrows(PayloadTooLargeException.class, () -> parse(raw));
        assertEquals(1024 * 1024 + 1, parse(raw, 2L * 1024 * 1024).body().length());
    }

    @Test
    void parseRequest_EnforcesMaximumLineLengthBoundary() throws IOException {
        String prefix = "X-Long: ";
        int maxLineSize = 8 * 1024;

        // 8192 bytes before LF (including the trailing CR) is accepted
        String accepted = "GET / HTTP/1.1\r\n"
                + prefix + "a".repeat(maxLineSize - prefix.length() - 1) + "\r\n\r\n";
        assertEquals(maxLineSize - prefix.length() - 1, parse(accepted).header("x-long").length());

        // 8193 bytes before LF is rejected
        String rejected = "GET / HTTP/1.1\r\n"
                + prefix + "a".repeat(maxLineSize - prefix.length()) + "\r\n\r\n";
        assertThrows(IOException.class, () -> parse(rejected));
    }

    private static Request parse(String raw) throws IOException {
        return parse(raw.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static Request parse(String raw, long maxBodyBytes) throws IOException {
        return Request.parseRequest(
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.ISO_8859_1)), maxBodyBytes);
    }

    private static Request parse(byte[] raw) throws IOException {
        return Request.parseRequest(new ByteArrayInputStream(raw));
    }
}
