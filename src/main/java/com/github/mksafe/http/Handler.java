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
            case POST -> new Response(Status.CREATED, "".getBytes()); // placeholder
            case UNKNOWN -> new Response(Status.UNKNOWN, "".getBytes()); // send 501 status code
        };
    }

    public Response handleGetMethod(Request request) {

        // Resolve the absolute resource path and load the payload from the class path
        String fullPath = "/" + resourceDir + request.getPath();

        // default to index.html
        if (request.getPath().equals("/")) {
            fullPath += "index.html";
        }

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
            return new Response(Status.NOT_FOUND, "".getBytes()); // 404 for Obscurity
        }

        try (InputStream inputStream = Handler.class.getResourceAsStream(canonicalPath)) {
            // Check for null instead of waiting for NullPointerException
            if (inputStream == null) {
                System.out.println("Can't find resource at " + fullPath);
                return new Response(Status.NOT_FOUND, "".getBytes());
            }

            byte[] payload = inputStream.readAllBytes();
            return new Response(Status.OK, payload);

        } catch (IOException e) {
            // Couldn't read resource
            System.err.println(e.getMessage());
            return new Response(Status.INTERNAL_SERVER_ERROR, "".getBytes());
        }
    }
}
