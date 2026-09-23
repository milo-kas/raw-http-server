package com.github.mksafe.http;

@FunctionalInterface
public interface RouteHandler {
    Response handle(Request request) throws Exception;
}
