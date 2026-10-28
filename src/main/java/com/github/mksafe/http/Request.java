package com.github.mksafe.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public record Request(
    Method method, String path, String query, Map<String, String> headers, RequestBody body) {

    private static final int MAX_LINE_SIZE = 8 * 1024;
    static final int DEFAULT_MAX_BODY_BYTES = 1024 * 1024;

    public Request {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        body = body != null ? body : RequestBody.empty();
    }

    public Request(Method method, String path, String query, Map<String, String> headers, byte[] payload) {
        this(method, path, query, headers, toBody(payload));
    }

    public Request(Method method, String path, String query, byte[] payload) {
        this(method, path, query, Map.of(), payload);
    }

    public Request(Method method, String path, byte[] payload) {
        this(method, path, null, Map.of(), payload);
    }

    public Request(Method method, String path) {
        this(method, path, null, Map.of(), RequestBody.empty());
    }

    private static RequestBody toBody(byte[] payload) {
        return payload == null ? RequestBody.empty() : RequestBody.ofBytes(payload);
    }

    public byte[] payload() {
        if (body.length() == 0) {
            return null;
        }
        try {
            return body.bytes(DEFAULT_MAX_BODY_BYTES);
        } catch (PayloadTooLargeException e) {
            throw new UncheckedIOException(e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read request body", e);
        }
    }

    public static Request parseRequest(InputStream inputStream) throws IOException {
        return parseRequest(inputStream, DEFAULT_MAX_BODY_BYTES);
    }

    public static Request parseRequest(InputStream inputStream, long maxBodyBytes) throws IOException {
        if (maxBodyBytes < 0) {
            throw new IllegalArgumentException("maxBodyBytes must be non-negative: " + maxBodyBytes);
        }
        inputStream = new BufferedInputStream(inputStream);

        Method parsedMethod;
        String parsedPath;

        // Read the request line (first line)
        String line = readLine(inputStream);
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

        Map<String, String> parsedHeaders = new HashMap<>();

        // Read HTTP headers until reaching an empty line (end of headers)
        while ((line = readLine(inputStream)) != null) {
            if (line.isEmpty()) break;

            int colon = line.indexOf(':');
            if (colon <= 0) {
                throw new IOException("Invalid header line: " + line);
            }

            String headerName = line.substring(0, colon).trim();
            String headerValue = line.substring(colon + 1).trim();

            if (headerName.isEmpty()) {
                throw new IOException("Invalid header line: " + line);
            }
            if (headerName.equalsIgnoreCase("content-length")
                && headerValue(parsedHeaders, headerName) != null) {
                throw new IOException("Duplicate Content-Length");
            }

            parsedHeaders.put(headerName, headerValue);
        }

        String contentLengthValue = headerValue(parsedHeaders, "content-length");
        long contentLength = 0;
        if (contentLengthValue != null) {
            try {
                contentLength = Long.parseLong(contentLengthValue);
            } catch (NumberFormatException e) {
                throw new IOException("Invalid Content-Length: " + contentLengthValue, e);
            }
            if (contentLength < 0) {
                throw new IOException("Negative Content-Length: " + contentLengthValue);
            }
            if (contentLength > maxBodyBytes) {
                throw new PayloadTooLargeException("Payload length " + contentLength
                    + " exceeds limit of " + maxBodyBytes + " bytes");
            }
        }

        RequestBody parsedBody = contentLength > 0
            ? RequestBody.streaming(inputStream, contentLength)
            : RequestBody.empty();

        return new Request(parsedMethod, parsedPath, parsedQuery, parsedHeaders, parsedBody);
    }

    /**
     * Reads a single CRLF-terminated line as bytes; bytes are preserved
     */
    private static String readLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int b;
        int lineLength = 0;

        while ((b = inputStream.read()) != -1 && b != '\n') {
            if (++lineLength > MAX_LINE_SIZE) {
                throw new IOException("HTTP line exceeded maximum length of " + MAX_LINE_SIZE + " bytes");
            }
            if (b != '\r') {
                line.write(b);
            }
        }

        // Clean EOF before any data arrived
        if (b == -1 && lineLength == 0) {
            return null;
        }

        // Connection closed prematurely
        if (b == -1) {
            throw new IOException("Connection closed prematurely before line termination");
        }

        return line.toString(StandardCharsets.ISO_8859_1);
    }

    /**
     * Retrieves a header value via a case-insensitive key.
     */
    public String header(String name) {
        return headerValue(headers, name);
    }

    private static String headerValue(Map<String, String> headers, String name) {
        if (name == null) return null;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
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
