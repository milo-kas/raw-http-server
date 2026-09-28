package com.github.mksafe.http;

public enum ContentType {
    HTML("text/html"),
    CSS("text/css"),
    PNG("image/png"),
    JPEG("image/jpeg"),
    ICO("image/x-icon"),
    JSON("application/json"),
    PLAIN("text/plain"),
    OCTET_STREAM("application/octet-stream");

    private final String contentType;

    ContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    // Helper to get the content type
    public static ContentType fromExtension(String extension) {
        return switch (extension.toLowerCase()) {
            case "html", "htm" -> HTML;
            case "css"         -> CSS;
            case "png"         -> PNG;
            case "jpg", "jpeg" -> JPEG;
            case "ico"         -> ICO;
            case "json"        -> JSON;
            case "txt"         -> PLAIN;
            default -> OCTET_STREAM;
        };
    }
}
