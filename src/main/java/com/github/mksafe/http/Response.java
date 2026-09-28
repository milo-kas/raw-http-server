package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

public class Response {

    private static final DateTimeFormatter RFC_9110_HTTP_DATE = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

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

    public Response(Status status, String contentType, byte[] payload, Method method) {
        this.status = status;
        this.contentType = contentType;
        this.cachedPayload = payload != null ? payload.clone() : null;
        this.contentLength = payload != null ? payload.length : 0;
        this.payload = null;
        this.method = method;
    }

    // Static Factory Helpers

    public static Response text(String text, Method method) {
        return text(Status.OK, text, method);
    }

    public static Response text(Status status, String text, Method method) {
        byte[] bytes = text != null ? text.getBytes(StandardCharsets.UTF_8) : null;
        return new Response(status, ContentType.PLAIN.getContentType(), bytes, method);
    }

    public static Response json(String json, Method method) {
        return json(Status.OK, json, method);
    }

    public static Response json(Status status, String json, Method method) {
        byte[] bytes = json != null ? json.getBytes(StandardCharsets.UTF_8) : null;
        return new Response(status, ContentType.JSON.getContentType(), bytes, method);
    }

    public static Response html(String html, Method method) {
        return html(Status.OK, html, method);
    }

    public static Response html(Status status, String html, Method method) {
        byte[] bytes = html != null ? html.getBytes(StandardCharsets.UTF_8) : null;
        return new Response(status, ContentType.HTML.getContentType(), bytes, method);
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
        if (!Method.HEAD.equals(method)) {
            if (cachedPayload != null) {
                outputStream.write(cachedPayload);
            } else if (payload != null) {
                try (InputStream stream = payload) {
                    stream.transferTo(outputStream);
                }
            }
        } else if (payload != null) {
            payload.close();
        }

        // Flush buffered bytes to be written to underlying socket
        outputStream.flush();
    }

    // CRLF helper
    private static byte[] formatHeader(String headerString) {
        return (headerString + "\r\n").getBytes(StandardCharsets.US_ASCII);
    }

    // Date helper with format as per RFC 9110
    private static String getDate() {
        return RFC_9110_HTTP_DATE.format(ZonedDateTime.now(ZoneId.of("GMT")));
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
