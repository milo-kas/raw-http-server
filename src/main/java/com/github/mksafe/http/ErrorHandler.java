package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

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

        URL errorUrl = ErrorHandler.class.getResource(errorPagePath);
        if (errorUrl != null) {
            try {
                URLConnection connection = errorUrl.openConnection();
                long contentLength = connection.getContentLengthLong();
                InputStream inputStream = connection.getInputStream();
                return new Response(status, ContentType.HTML.getContentType(), inputStream, contentLength, method);
            } catch (IOException e) {
                System.err.printf("Failed to read custom error page %s: %s%n", errorPagePath, e.getMessage());
            }
        }

        // File wasn't found or couldn't be read, send a plain message
        String message = status.getCode() + " " + status.getMessage();
        return new Response(status, "text/plain", message.getBytes(), method);
    }
}
