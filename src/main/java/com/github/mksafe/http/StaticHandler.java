package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StaticHandler {

    private final String resourceDir;
    private final ErrorHandler errorHandler;

    public StaticHandler(String resourceDir, ErrorHandler errorHandler) {
        this.resourceDir = resourceDir;
        this.errorHandler = errorHandler;
    }

    public StaticHandler(String resourceDir) {
        this(resourceDir, new ErrorHandler(resourceDir));
    }

    public Response handleStaticResource(Request request) {
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
            return errorHandler.createErrorResponse(Status.NOT_FOUND, request.getMethod(), request.getPath()); // 404 for Obscurity
        }

        try (InputStream inputStream = StaticHandler.class.getResourceAsStream(canonicalPath)) {
            // Check for null instead of waiting for NullPointerException
            if (inputStream == null) {
                System.out.println("Can't find resource at " + fullPath);
                return errorHandler.createErrorResponse(Status.NOT_FOUND, request.getMethod(), request.getPath());
            }

            byte[] payload = inputStream.readAllBytes();
            return new Response(Status.OK, contentType, payload, request.getMethod());

        } catch (IOException e) {
            // Couldn't read resource
            System.err.println(e.getMessage());
            return errorHandler.createErrorResponse(Status.NOT_FOUND, request.getMethod(), request.getPath());
        }
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
