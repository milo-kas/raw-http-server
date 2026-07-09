package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;

public class Handler {

    private final String resourceDir;

    public Handler(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    public Response handleRequest(Request request) {
        // TODO: path, payload; POST, UNKNOWN, different status
        // TODO: Complete implementation of responses like 501 status response
        return switch (request.getMethod()) {
            case GET -> handleGetMethod(request);
            case POST -> new Response(201, "".getBytes()); // placeholder
            case UNKNOWN -> new Response(501, "".getBytes()); // send 501 status code
        };
    }

    public Response handleGetMethod(Request request) {
        // Resolve the absolute resource path and load the payload from the class path
        String fullPath = "/" + resourceDir + request.getPath();
        System.out.println("--- full path: " + fullPath);

        try (InputStream inputStream = Handler.class.getResourceAsStream(fullPath)) {
            // Check for null instead of waiting for NullPointerException
            if (inputStream == null) {
                System.out.println("Can't find resource at " + fullPath);
                return new Response(404, "".getBytes());
            }

            byte[] payload = inputStream.readAllBytes();
            return new Response(200, payload);

        } catch (IOException e) {
            // Couldn't read resource
            System.err.println(e.getMessage());
            return new Response(500, "".getBytes());
        }
    }
}
