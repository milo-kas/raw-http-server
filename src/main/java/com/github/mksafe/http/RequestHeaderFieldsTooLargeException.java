package com.github.mksafe.http;

import java.io.IOException;

public class RequestHeaderFieldsTooLargeException extends IOException {

    public RequestHeaderFieldsTooLargeException(String message) {
        super(message);
    }
}
