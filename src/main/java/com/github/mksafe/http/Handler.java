package com.github.mksafe.http;

import java.io.IOException;

public class Handler {

    public void handleResponse(Request request, Response response) throws IOException {
        // TODO: path, payload; POST, UNKNOWN, different status
        switch (request.getMethod()) {
            case GET -> response.respond();
            case POST -> System.out.println("TODO");
            case UNKNOWN -> System.out.println("UNKNOWN"); // send 501 status code
        }
    }
}
