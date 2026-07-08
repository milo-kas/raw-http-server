package com.github.mksafe.http;

import java.io.IOException;
import java.io.OutputStream;

public class Response {

    private final OutputStream outputStream;

    Response(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    // Send actual response
    public void respond() throws IOException {
        // status
        // content type
        // CRLF
        // payload

        // Stream back a successful response (status code 200 - 299)
        outputStream.write(formatHeader("HTTP/1.1 200 OK"));
        // Stream content type
        outputStream.write(formatHeader("Content-Type: text/html"));
        // Stream empty line to signal the end of headers
        outputStream.write(formatHeader(""));
        // Send headers
        outputStream.flush();

        // TODO: Reading from file
        // Stream payload
        outputStream.write("<h1>HELLO WORLD!</h1> <h2>testing</h2>".getBytes());
        outputStream.flush();
    }

    // CRLF helper
    private static byte[] formatHeader(String headerString) {
        return (headerString +  "\r\n").getBytes();
    }
}
