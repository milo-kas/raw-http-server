package com.github.mksafe.http;

import java.io.IOException;

public class Handler {

    public Response handleRequest(Request request) throws IOException {
        // TODO: path, payload; POST, UNKNOWN, different status
        // TODO: Complete implementation of responses like 501 status response
        return switch (request.getMethod()) {
            case GET -> new Response(200);
            case POST -> new Response(201); // placeholder
            case UNKNOWN -> new Response(501); // send 501 status code
        };
    }
}
