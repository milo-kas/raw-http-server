package com.github.mksafe.http;

public enum Method {
    GET, HEAD, POST, UNKNOWN;

    public static Method fromString(String method) {
        if (method == null) return UNKNOWN;

        return switch (method.toUpperCase()) {
            case "GET" -> GET;
            case "HEAD" -> HEAD;
            case "POST" -> POST;
            default -> UNKNOWN;
        };
    }
}
