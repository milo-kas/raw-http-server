package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private int port = 8080;

    public Server() {}

    public Server(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);

        // creater handler
        Handler httpHandler = new Handler();

        for (int i = 0; true; i++) {
            Socket clientSocket = serverSocket.accept();

            System.out.println("--- waiting for request #" + i); // - #1 (next req) is for favicon.ico
            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            Request request = new Request(bufferedReader);

            OutputStream outputStream = clientSocket.getOutputStream();

            Response response = new Response(outputStream);

            httpHandler.handleResponse(request, response);

            clientSocket.close();
        }
    }
}
