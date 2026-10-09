package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.IOException;

import java.net.URI;


public record Request(Method method, String path, String payload) {

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

        return new Request(parsedMethod, parsedPath, parsedPayload);
    }
}
