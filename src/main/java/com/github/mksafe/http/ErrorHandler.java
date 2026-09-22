package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;

public class ErrorHandler {

    private final String resourceDir;

    public ErrorHandler(String resourceDir) {
        this.resourceDir = resourceDir;
    }

    public Response createErrorResponse(Status status, Method method, String path) {
        if (path != null && path.startsWith("/api/")) {
            String json = "{\"error\": \"" + status.getMessage() + "\"}\n";
            return new Response(status, "application/json", json.getBytes(), method);
        }

        // Not an API path
        return createErrorResponse(status, method);
    }

    public Response createErrorResponse(Status status, Method method) {
        // Dynamic error pages based on status code
        String errorPagePath = "/" + resourceDir + "/error/" + status.getCode() + ".html";

        try (InputStream errorStream = ErrorHandler.class.getResourceAsStream(errorPagePath)) {
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
}
