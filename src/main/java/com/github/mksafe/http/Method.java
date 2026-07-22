package com.github.mksafe.http;

public enum Method {
    GET, HEAD, POST, UNKNOWN;

    Method setMethod(String method) {
        try {
            return Method.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}