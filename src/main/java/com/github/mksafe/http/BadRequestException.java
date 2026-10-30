package com.github.mksafe.http;

import java.io.IOException;

public class BadRequestException extends IOException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
