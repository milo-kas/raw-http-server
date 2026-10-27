package com.github.mksafe.http;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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

    private static Request parse(String raw) throws IOException {
        return Request.parseRequest(
                new ByteArrayInputStream(raw.getBytes(StandardCharsets.ISO_8859_1)));
    }
}
