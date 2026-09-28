package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Response {

    private final Status status;
    private final String contentType;
    private final long contentLength;
    private final Method method;
    private final InputStream payload;
    private byte[] cachedPayload;

    public Response(Status status, String contentType, InputStream payload, long contentLength, Method method) {
        this.status = status;
        this.contentType = contentType;
        this.payload = payload;
        this.contentLength = contentLength;
        this.method = method;
    }

    public Response(Status status, String contentType, InputStream payload, Method method) {
        this(status, contentType, payload, -1, method);
    }

    public Response(Status status, String contentType, byte[] payload, Method method) {
        this.status = status;
        this.contentType = contentType;
        this.cachedPayload = payload != null ? payload.clone() : null;
        this.contentLength = payload != null ? payload.length : 0;
        this.payload = null;
        this.method = method;
    }

    // Send actual response
    public void respond(OutputStream outputStream) throws IOException {
        // Stream the response status line
        outputStream.write(formatHeader("HTTP/1.1 " + status.getCode() + " " + status.getMessage()));
        // Stream content type
        outputStream.write(formatHeader("Content-Type: " + contentType));
        if (contentLength >= 0) {
            outputStream.write(formatHeader("Content-Length: " + contentLength));
        }
        // Signal the end of the TCP connection after response
        outputStream.write(formatHeader("Connection: close"));
        // Stream date
        outputStream.write(formatHeader("Date: " + getDate()));

        // Security headers
        outputStream.write(formatHeader("X-Content-Type-Options: nosniff"));
        outputStream.write(formatHeader("X-Frame-Options: DENY"));

        // Stream empty line to signal the end of headers
        outputStream.write(formatHeader(""));

        // Stream payload if requested and method is not HEAD
        if (!method.equals(Method.HEAD)) {
            if (cachedPayload != null) {
                outputStream.write(cachedPayload);
            } else if (payload != null) {
                try (InputStream stream = payload) {
                    stream.transferTo(outputStream);
                }
            }
        } else if (payload != null) {
            try (InputStream stream = payload) {
                // Safely close the input stream
            }
        }

        // Flush buffered bytes to be written to underlying socket
        outputStream.flush();
    }

    // CRLF helper
    private static byte[] formatHeader(String headerString) {
        return (headerString + "\r\n").getBytes();
    }

    // Date helper with format as per RFC 9110
    private static String getDate() {
        DateTimeFormatter RFC_9110_HTTP_DATE = DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT"));
        return RFC_9110_HTTP_DATE.format(zonedDateTime);
    }

    // Getters

    public Status getStatus() {
        return status;
    }

    public byte[] getPayload() {
        if (cachedPayload != null) {
            return cachedPayload.clone();
        }
        if (payload == null) {
            return null;
        }
        try {
            cachedPayload = payload.readAllBytes();
            return cachedPayload.clone();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read payload", e);
        }
    }

    public InputStream getPayloadStream() {
        return payload;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public Method getMethod() {
        return method;
    }
}
