package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class StaticHandler {

    private static final System.Logger log = System.getLogger(StaticHandler.class.getName());

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
        String fullPath = "/" + resourceDir + request.path();

        // default to index.html
        if (fullPath.endsWith("/")) {
            fullPath += "index.html";
        }

        // Dot segments are normalised again to ensure containment and prevent upstream bypasses or parsing bugs
        Path path;
        try {
            path = Path.of(fullPath).normalize();
        } catch (InvalidPathException e) {
            log.log(System.Logger.Level.WARNING, "Invalid static resource path: {0}", request.path());
            return errorHandler.createErrorResponse(Status.BAD_REQUEST, request.method(), request.path());
        }

        // Check for path traversal; the path is empty or doesn't start with the resource directory
        if (path.getNameCount() == 0 || !path.getName(0).toString().equals(resourceDir)) {
            log.log(System.Logger.Level.WARNING, "Path traversal attempt: {0}", request.path());
            return errorHandler.createErrorResponse(Status.NOT_FOUND, request.method(), request.path()); // 404 for Obscurity
        }

        // Get full canonical path as string
        String canonicalPath = path.toString();

        // Account for windows dir formatting
        canonicalPath = canonicalPath.replace('\\', '/');

        // Get the content type
        String contentType = parseContentType(canonicalPath);

        URL resourceUrl = StaticHandler.class.getResource(canonicalPath);
        if (resourceUrl == null) {
            log.log(System.Logger.Level.DEBUG, "Resource not found: {0}", canonicalPath);
            return errorHandler.createErrorResponse(Status.NOT_FOUND, request.method(), request.path());
        }

        try {
            URLConnection connection = resourceUrl.openConnection();
            long contentLength = connection.getContentLengthLong();
            InputStream inputStream = connection.getInputStream();
            return new Response(Status.OK, contentType, inputStream, contentLength, request.method());
        } catch (IOException e) {
            // Couldn't read resource
            log.log(System.Logger.Level.ERROR, "Failed to read resource " + canonicalPath, e);
            return errorHandler.createErrorResponse(Status.NOT_FOUND, request.method(), request.path());
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
