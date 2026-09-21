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
        return switch (request.getMethod()) {
            case GET, HEAD -> handleGetMethod(request);
            case POST -> handlePostMethod(request);
            case UNKNOWN -> createErrorResponse(Status.UNKNOWN, request.getMethod()); // send 501 status code
        };
    }

    public Response handleGetMethod(Request request) {

        // Resolve the absolute resource path and load the payload from the class path
        String fullPath = "/" + resourceDir + request.getPath();

        // default to index.html
        if (fullPath.endsWith("/")) {
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
            return createErrorResponse(Status.NOT_FOUND, request.getMethod()); // 404 for Obscurity
        }

        try (InputStream inputStream = Handler.class.getResourceAsStream(canonicalPath)) {
            // Check for null instead of waiting for NullPointerException
            if (inputStream == null) {
                System.out.println("Can't find resource at " + fullPath);
                return createErrorResponse(Status.NOT_FOUND, request.getMethod());
            }

            byte[] payload = inputStream.readAllBytes();
            return new Response(Status.OK, contentType, payload, request.getMethod());

        } catch (IOException e) {
            // Couldn't read resource
            System.err.println(e.getMessage());
            return createErrorResponse(Status.NOT_FOUND, request.getMethod());
        }
    }

    private Response handlePostMethod(Request request) {
        String path = request.getPath();

        // Check for api echo test
        if (path.equals("/api/echo")) {
            String payload = request.getPayload();

            // No payload provided => bad request
            if (payload == null || payload.isBlank()) {
                return new Response(Status.BAD_REQUEST, "text/plain", "Missing payload".getBytes(), request.getMethod());
            }

            // Echo back the payload
            return new Response(Status.CREATED, "text/plain",  payload.getBytes(), request.getMethod());

        }

        // TODO: make this into createErrorResponse for POST method
        return new Response(Status.NOT_FOUND, "text/plain", "Not found".getBytes(), request.getMethod());
    }

    private Response createErrorResponse(Status status, Method method) {
        // Dynamic error pages based on status code
        String errorPagePath = "/" + resourceDir + "/error/" + status.getCode() + ".html";

        try (InputStream errorStream = Handler.class.getResourceAsStream(errorPagePath)) {
            // Serve the custom HTML file if it exists
            if (errorStream != null) {
                byte[] payload = errorStream.readAllBytes();
                return new Response(status, ContentType.HTML.getContentType(), payload, method);
            }
        } catch (IOException e) {
            System.err.printf("Failed to read custom error page %s: %s%n", errorPagePath, e.getMessage());
        }

        // File wasn't found or couldn't be read, send a plain message
        String message = status.getCode() + " " + status.getMessage();
        return new Response(status, "text/plain", message.getBytes(), method);
    }

    // Content type helper
    private String parseContentType(String canonicalPath) {
        // Get index of last '.' and '/' of the path
        int lastDot = canonicalPath.lastIndexOf('.');
        int lastSlash = Math.max(canonicalPath.lastIndexOf('/'), canonicalPath.lastIndexOf('\\'));

        // If the dot is truly in the filename and not a dir, then parse its extension
        if (lastDot > lastSlash) {
            String extension = canonicalPath.substring(lastDot + 1);

            return ContentType.fromExtension(extension).getContentType();
        }

        return ContentType.OCTET_STREAM.getContentType();
    }
}
