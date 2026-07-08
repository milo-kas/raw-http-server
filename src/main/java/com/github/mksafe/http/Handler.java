package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;

public class Handler {

    private final String resourceDir;

    public Handler(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    public Response handleRequest(Request request) throws IOException {

        // Resolve the absolute resource path and load the payload from the class path
        String fullPath = "/" + resourceDir + request.getPath();
        System.out.println("--- full path: " + fullPath);
        InputStream inputStream = Handler.class.getResourceAsStream(fullPath);
        byte[] payload = inputStream.readAllBytes();

        // TODO: path, payload; POST, UNKNOWN, different status
        // TODO: Complete implementation of responses like 501 status response
        return switch (request.getMethod()) {
            case GET -> new Response(200, payload);
            case POST -> new Response(201, "".getBytes()); // placeholder
            case UNKNOWN -> new Response(501, "".getBytes()); // send 501 status code
        };
    }
}
