package com.github.mksafe.http;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String resourceDir = "public";

        Server server = new Server(port, resourceDir);
        server.start();
    }
}