package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.IOException;

import java.net.URI;


public class Request {

    private Method method;
    private String path;
    private String payload;

    public Request(BufferedReader bufferedReader) throws IOException {
        parseRequest(bufferedReader);
    }

    private void parseRequest(BufferedReader bufferedReader) throws IOException {

        // Get the actual request (first line)
        String line = bufferedReader.readLine();

        // Parse actual request
        String[] request = line.split(" ");

        try {
            method = Method.valueOf(request[0]);
        } catch (IllegalArgumentException e) {
            method = Method.UNKNOWN;
        }

        String rawPath = request[1];

        // Decode path
        URI uri = URI.create(rawPath);
        path = uri.getPath();

        int contentLength = 0;

        // Read HTTP headers until reaching an empty line (end of headers)
        while((line = bufferedReader.readLine()) != null) {
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
                payload = new String(bodyChars, 0, charsRead);
                System.out.println("Payload present!");
            }
        }
    }

    // Getter for http client request method
    public Method getMethod() {
        return method;
    }

    // Getter for file path
    public String getPath() {
        return path;
    }

    public String getPayload() { return payload; }
}
