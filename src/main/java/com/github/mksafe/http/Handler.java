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
        // TODO: Add HEAD method
        // TODO: Complete implementation of responses like 501 status response
        return switch (request.getMethod()) {
            case GET -> handleGetMethod(request);
            case POST -> new Response(Status.CREATED, "text/plain", "".getBytes()); // placeholder
            case UNKNOWN -> new Response(Status.UNKNOWN, "text/plain", "".getBytes()); // send 501 status code
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

        // Get the content type
        String contentType = parseContentType(canonicalPath);

        // Check for path traversal; the path is empty or doesn't start with the resource directory
        if (path.getNameCount() == 0 || !path.getName(0).toString().equals(resourceDir)) {
            System.err.println("Path Traversal Detected!");
            return new Response(Status.NOT_FOUND, "text/plain", "".getBytes()); // 404 for Obscurity
        }

        try (InputStream inputStream = Handler.class.getResourceAsStream(canonicalPath)) {
            // Check for null instead of waiting for NullPointerException
            if (inputStream == null) {
                System.out.println("Can't find resource at " + fullPath);
                return new Response(Status.NOT_FOUND, "text/plain", "".getBytes());
            }

            byte[] payload = inputStream.readAllBytes();
            return new Response(Status.OK, contentType, payload);

        } catch (IOException e) {
            // Couldn't read resource
            System.err.println(e.getMessage());
            return new Response(Status.INTERNAL_SERVER_ERROR, "text/plain", "".getBytes());
        }
    }


    // Content type helper
    private String parseContentType(String canonicalPath) {
        // Get index of last '.' and '/' of the path
        int lastDot = canonicalPath.lastIndexOf('.');
        int lastSlash = Math.max(canonicalPath.lastIndexOf('/'), canonicalPath.lastIndexOf('\\'));

        // If the dot is truly in the filename and not a dir, then parse its extension
        if (lastDot > lastSlash) {
            String extension = canonicalPath.substring(lastDot + 1).toLowerCase();

            return switch (extension) {
                case "html", "htm" -> "text/html";
                case "css"         -> "text/css";
                case "png"         -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                case "ico"         -> "image/x-icon";
                default -> "application/octet-stream";
            };
        }

        return "application/octet-stream";
    }
}
