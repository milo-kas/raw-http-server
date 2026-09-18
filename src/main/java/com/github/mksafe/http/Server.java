package com.github.mksafe.http;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private final int port;
    private final String resourceDir;

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

        // Create handler outside loop
        Handler handler = new Handler(resourceDir);

        // Project Loom
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        for (int i = 0; true; i++) {
            Socket clientSocket = serverSocket.accept();

            // Local variables in executor lambda must be effectively final
            final int requestNum = i;

            // Wrap in virtual thread submission
            executor.submit(() -> handleClient(clientSocket, requestNum, handler));
        }
    }

    private void handleClient(Socket clientSocket, int requestNum, Handler handler) {
        try (clientSocket) {
            System.out.println("--- waiting for request #" + requestNum);

            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            Request request = Request.parseRequest(bufferedReader);
            OutputStream outputStream = clientSocket.getOutputStream();

            Response response = handler.handleRequest(request);
            response.respond(outputStream);

        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
    }
}
