package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private final int port;
    private final Router router;

    public Server(Router router) {
        this(8080, router);
    }

    public Server(int port, Router router) {
        this.port = port;
        this.router = router;
    }

    public Server(int port, String resourcePath) {
        this(port, new Router(resourcePath));
    }

    public Server(String resourcePath) {
        this(8080, resourcePath);
    }

    public Server(int port) {
        this(port, "public");
    }

    public Server() {
        this(8080, "public");
    }

    public void start() throws IOException {

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            ServerSocket serverSocket = new ServerSocket(port);

            int requestNum = 0;

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();

                final int requestId = ++requestNum;
                executor.submit(() -> handleClient(clientSocket, requestId, router));
            }
        }
    }

    private void handleClient(Socket clientSocket, int requestNum, Router router) {
        try (clientSocket) {
            System.out.println("--- waiting for request #" + requestNum);

            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            Request request = Request.parseRequest(bufferedReader);
            OutputStream outputStream = clientSocket.getOutputStream();

            Response response = router.handleRequest(request);
            response.respond(outputStream);

        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
    }
}
