package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        // Flatten any relative path into a canonical destination
        Path path = Paths.get(fullPath).normalize();

        // Get full canonical path as string
        String canonicalPath = path.toString();

        // Account for windows dir formatting
        canonicalPath = canonicalPath.replace('\\', '/');

        System.out.println("Final path: " + canonicalPath);

        // Check for path traversal; the path is empty or doesn't start with the resource directory
        if (path.getNameCount() == 0 || !path.getName(0).toString().equals(resourceDir)) {
            System.err.println("Path Traversal Detected!");
            return new Response(404, "".getBytes()); // 404 for Obscurity
        }

        try (InputStream inputStream = Handler.class.getResourceAsStream(canonicalPath)) {
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
