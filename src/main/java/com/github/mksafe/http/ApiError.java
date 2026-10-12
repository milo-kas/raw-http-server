package com.github.mksafe.http;

public record ApiError(String error) {

    public String toJson() {
        return """
                {"error": "%s"}
                """.formatted(escapeJson(error)).stripTrailing();
    }

    private static String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
