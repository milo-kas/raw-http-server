package com.github.mksafe.http;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestBodyTest {

    @Test
    void ofBytes_CopiesInput() throws IOException {
        byte[] original = "body".getBytes(StandardCharsets.UTF_8);
        RequestBody body = RequestBody.ofBytes(original);

        original[0] = 'X';

        assertArrayEquals("body".getBytes(StandardCharsets.UTF_8), body.bytes(4));
    }

    @Test
    void bytes_RejectsBodyLargerThanLimit() {
        RequestBody body = RequestBody.ofBytes("body".getBytes(StandardCharsets.UTF_8));

        assertThrows(PayloadTooLargeException.class, () -> body.bytes(3));
    }

    @Test
    void stream_IsBoundedToDeclaredLength() throws IOException {
        RequestBody body = RequestBody.streaming(
                new ByteArrayInputStream("abcdef".getBytes(StandardCharsets.UTF_8)), 3);

        try (InputStream stream = body.stream()) {
            assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), stream.readAllBytes());
        }
    }

    @Test
    void stream_DoesNotCloseUnderlyingStream() throws IOException {
        TrackingInputStream input = new TrackingInputStream("abc".getBytes(StandardCharsets.UTF_8));

        RequestBody.streaming(input, 3).stream().close();

        assertFalse(input.closed);
    }

    @Test
    void bytes_RejectsTruncatedStream() {
        RequestBody body = RequestBody.streaming(
                new ByteArrayInputStream("ab".getBytes(StandardCharsets.UTF_8)), 5);

        assertThrows(EOFException.class, () -> body.bytes(5));
    }

    @Test
    void empty_HasNoContent() throws IOException {
        RequestBody body = RequestBody.empty();

        assertEquals(0, body.length());
        assertEquals(-1, body.stream().read());
        assertEquals(0, body.bytes(0).length);
    }

    @Test
    void string_DecodesUtf8() throws IOException {
        RequestBody body = RequestBody.ofBytes("héllo".getBytes(StandardCharsets.UTF_8));

        assertEquals("héllo", body.string(6));
    }

    @Test
    void ofBytes_RejectsNull() {
        assertThrows(NullPointerException.class, () -> RequestBody.ofBytes(null));
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] buffer) {
            super(buffer);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
