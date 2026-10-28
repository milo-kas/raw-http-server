package com.github.mksafe.http;

import java.io.IOException;

public class PayloadTooLargeException extends IOException {

    public PayloadTooLargeException(String message) {
        super(message);
    }
}
