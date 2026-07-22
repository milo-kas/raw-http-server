package com.github.mksafe.http;

import java.io.IOException;
import java.io.OutputStream;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Response {

    private final Status status;
    private final byte[] payload;
    private final String contentType;
    private final Method method;

    Response(Status status, String contentType, byte[] payload, Method method) {
        this.status = status;
        this.contentType = contentType;
        this.payload = payload;
        this.method = method;
    }

    // Send actual response
    public void respond(OutputStream outputStream) throws IOException {
        // Stream the response status line
        outputStream.write(formatHeader("HTTP/1.1 " + status.getCode() + " "+ status.getMessage()));
        // Stream content type
//        outputStream.write(formatHeader("Content-Type: text/html"));
        outputStream.write(formatHeader("Content-Type: " + contentType));
        outputStream.write(formatHeader("Content-Length: " + payload.length));
        // Signal the end of the TCP connection after response
        outputStream.write(formatHeader("Connection: close"));
        // Stream date
        outputStream.write(formatHeader("Date: " + getDate()));

        // Stream empty line to signal the end of headers
        outputStream.write(formatHeader(""));

        // Stream payload if requested
        if (method.equals(Method.GET)) {
            outputStream.write(payload);
        }

        // Flush buffered bytes to be written to underlying socket
        outputStream.flush();
    }

    // CRLF helper
    private static byte[] formatHeader(String headerString) {
        return (headerString +  "\r\n").getBytes();
    }

    // Date helper with format as per RFC 9110
    private static String getDate() {
        DateTimeFormatter RFC_9110_HTTP_DATE = DateTimeFormatter
                .ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH).withZone(ZoneId.of("GMT"));

        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT"));
        return RFC_9110_HTTP_DATE.format(zonedDateTime);
    }
}
