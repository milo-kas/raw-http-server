package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.IOException;

public class Request {

    public Method method;

    public Request(BufferedReader bufferedReader) throws IOException {
        parseRequest(bufferedReader);
    }

    private void parseRequest(BufferedReader bufferedReader) throws IOException {

        // Get the actual request (first line)
        String line = bufferedReader.readLine();

        // Parse actual request
        String[] request = line.split(" ");

        method = Method.valueOf(request[0]);

        // Read HTTP headers until reaching an empty line (end of headers)
        while((line = bufferedReader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }

            System.out.println(line);
        }

        // Check for payload -- check by length instead
//        if (bufferedReader.readLine() != null) {
//            // payload
//            System.out.println("Payload present! ----");
//        }

    }

    // Getter for http client request method
    public Method getMethod() {
        return method;
    }
}
