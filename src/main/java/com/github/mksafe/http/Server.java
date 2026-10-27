package com.github.mksafe.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final System.Logger log = System.getLogger(Server.class.getName());

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
            log.log(System.Logger.Level.INFO, "Listening on port " + port);

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
            log.log(System.Logger.Level.DEBUG,
                    "Connection #" + requestNum + " from " + clientSocket.getRemoteSocketAddress());

            // Read and parse the incoming raw HTTP request as a byte stream
            Request request = Request.parseRequest(clientSocket.getInputStream());

            log.log(System.Logger.Level.INFO,
                    "Connection #" + requestNum + ": " + request.method() + " " + request.path());

            OutputStream outputStream = clientSocket.getOutputStream();

            Response response = router.handleRequest(request);
            response.respond(outputStream);

        } catch (Exception e) {
            log.log(System.Logger.Level.ERROR, "Error handling request #" + requestNum, e);
        }
    }
}
