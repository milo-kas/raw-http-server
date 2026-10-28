package com.github.mksafe.http;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class RequestBody {

    private static final RequestBody EMPTY = new RequestBody(0, InputStream.nullInputStream());

    private final long length;
    private final InputStream stream;

    private RequestBody(long length, InputStream stream) {
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative: " + length);
        }
        this.length = length;
        this.stream = Objects.requireNonNull(stream, "stream must not be null");
    }

    public static RequestBody empty() {
        return EMPTY;
    }

    public static RequestBody ofBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes must not be null");
        if (bytes.length == 0) {
            return EMPTY;
        }
        byte[] copy = bytes.clone();
        return new RequestBody(copy.length, new ByteArrayInputStream(copy));
    }

    static RequestBody streaming(InputStream stream, long length) {
        Objects.requireNonNull(stream, "stream must not be null");
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative: " + length);
        }
        return new RequestBody(length, new BoundedInputStream(stream, length));
    }

    public long length() {
        return length;
    }

    public InputStream stream() {
        return stream;
    }

    public byte[] bytes(int maxBytes) throws IOException {
        if (maxBytes < 0) {
            throw new IllegalArgumentException("maxBytes must be non-negative");
        }
        if (length > maxBytes) {
            throw new PayloadTooLargeException(
                    "Payload length " + length + " exceeds limit of " + maxBytes + " bytes");
        }
        if (length == 0) {
            return new byte[0];
        }

        byte[] data = stream.readNBytes((int) length);
        if (data.length != length) {
            throw new EOFException("Unexpected end of payload: expected " + length
                    + " bytes, got " + data.length);
        }
        return data;
    }

    public String string(int maxBytes) throws IOException {
        return new String(bytes(maxBytes), StandardCharsets.UTF_8);
    }

    private static final class BoundedInputStream extends InputStream {

        private final InputStream inputStream;
        private long remaining;

        private BoundedInputStream(InputStream inputStream, long limit) {
            this.inputStream = inputStream;
            this.remaining = limit;
        }

        @Override
        public int read() throws IOException {
            if (remaining == 0) {
                return -1;
            }
            int read = inputStream.read();
            if (read != -1) {
                remaining--;
            }
            return read;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            if (remaining == 0) {
                return -1;
            }
            int read = inputStream.read(buffer, offset, (int) Math.min(length, remaining));
            if (read > 0) {
                remaining -= read;
            }
            return read;
        }

        @Override
        public void close() {
        }
    }
}
