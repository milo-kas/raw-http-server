package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.IOException;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public record Request(Method method, String path, String query, String payload) {

    public Request(Method method, String path, String payload) {
        this(method, path, null, payload);
    }

    public Request(Method method, String path) {
        this(method, path, null, null);
    }

    public static Request parseRequest(BufferedReader bufferedReader) throws IOException {
        Method parsedMethod;
        String parsedPath;
        String parsedPayload = null;

        // Read the request line (first line)
        String line = bufferedReader.readLine();
        // Abort if the connection drops early or sends an empty line
        if (line == null || line.isBlank()) {
            throw new IOException("Empty Request");
        }

        // Tokenise on whitespace and ensure that the method and path exist
        String[] request = line.split("\\s+");
        if (request.length < 2) {
            throw new IOException("Invalid Request Line: " + line);
        }

        parsedMethod = Method.fromString(request[0]);

        String rawPath = request[1];

        // Decode path
        URI uri = URI.create(rawPath);
        parsedPath = uri.getPath();
        String parsedQuery = uri.getRawQuery();

        int contentLength = 0;

        // Read HTTP headers until reaching an empty line (end of headers)
        while ((line = bufferedReader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }

            // check for content length
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
            }

            System.out.println(line);
        }

        // Check for payload - read contentLength chars
        if (contentLength > 0) {
            char[] bodyChars = new char[contentLength];
            int charsRead = bufferedReader.read(bodyChars, 0, bodyChars.length);

            if (charsRead > 0) {
                parsedPayload = new String(bodyChars, 0, charsRead);
                System.out.println("Payload present!");
            }
        }

        return new Request(parsedMethod, parsedPath, parsedQuery, parsedPayload);
    }

    /**
     * Retrieves a single parameter value by name.
     * Returns null if the parameter or query string is absent.
     */
    public String queryParam(String name) {
        if (query == null || query.isBlank() || name == null) {
            return null;
        }
        return queryParams().get(name);
    }

    /**
     * Parses the query string into an unmodifiable map of key-value pairs.
     */
    public Map<String, String> queryParams() {
        if (query == null || query.isBlank()) {
            return Collections.emptyMap();
        }

        String raw = query.startsWith("?") ? query.substring(1) : query;
        if (raw.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new HashMap<>();
        String[] pairs = raw.split("&");

        for (String pair : pairs) {
            if (pair.isBlank()) continue;

            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = decode(pair.substring(0, idx));
                String value = decode(pair.substring(idx + 1));
                params.put(key, value);
            } else if (idx == -1) {
                String key = decode(pair);
                params.put(key, "");
            }
        }

        return Collections.unmodifiableMap(params);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
