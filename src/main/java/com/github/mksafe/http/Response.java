package com.github.mksafe.http;

import java.io.IOException;
import java.io.OutputStream;

public class Response {

    private final Status status;
    private final byte[] payload;

    Response(Status status, byte[] payload) {
        this.status = status;
        this.payload = payload;
    }

    // Send actual response
    public void respond(OutputStream outputStream) throws IOException {
        // TODO: Support custom responses

        // status
        // content type
        // CRLF
        // payload

        // Stream back a successful response (status code 200 - 299)
        outputStream.write(formatHeader("HTTP/1.1 " + status.getCode() + " "+ status.getMessage()));
        // Stream content type
        outputStream.write(formatHeader("Content-Type: text/html"));
        // Stream empty line to signal the end of headers
        outputStream.write(formatHeader(""));
        // Send headers
        outputStream.flush();

        // Stream payload
        outputStream.write(payload);
        outputStream.flush();
    }

    // CRLF helper
    private static byte[] formatHeader(String headerString) {
        return (headerString +  "\r\n").getBytes();
    }
}
