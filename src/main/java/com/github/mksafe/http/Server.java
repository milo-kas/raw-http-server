package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port;
    private String resourceDir;

    public Server() {
        this(8080, "public");
    }

    public Server(int port) {
        this(port, "public");
    }
    public Server(String resourcePath) {
        this(8080, resourcePath);
    }

    public Server(int port, String resourcePath) {
        this.port = port;
        this.resourceDir = resourcePath;
    }

    public void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);

        // creater handler
        Handler handler = new Handler(resourceDir);

        for (int i = 0; true; i++) {
            Socket clientSocket = serverSocket.accept();

            System.out.println("--- waiting for request #" + i); // - #1 (next req) is for favicon.ico
            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            Request request = new Request(bufferedReader);

            OutputStream outputStream = clientSocket.getOutputStream();

            Response response = handler.handleRequest(request);

            response.respond(outputStream);

            clientSocket.close();
        }
    }
}
