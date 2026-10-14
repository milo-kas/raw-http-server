package com.github.mksafe.http;

import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void parseRequest_ExtractsPathQueryAndPayload() throws IOException {
        String raw = "POST /api/users?page=2&tag=c%23 HTTP/1.1\r\n" +
                     "Content-Length: 4\r\n\r\n" +
                     "data";
        Request req = Request.parseRequest(new BufferedReader(new StringReader(raw)));

        assertEquals(Method.POST, req.method());
        assertEquals("/api/users", req.path());
        assertEquals("2", req.queryParam("page"));
        assertEquals("c#", req.queryParam("tag"));
        assertEquals("data", req.payload());
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
        assertThrows(IOException.class, () -> Request.parseRequest(new BufferedReader(new StringReader(""))));
        assertThrows(IOException.class, () -> Request.parseRequest(new BufferedReader(new StringReader("BAD\r\n\r\n"))));
    }
}
