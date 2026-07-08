package com.github.mksafe.http;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        Server server = new Server(port);
        server.start();
    }
}