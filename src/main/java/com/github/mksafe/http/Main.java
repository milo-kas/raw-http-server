package com.github.mksafe.http;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String resourceDir = "public";

        Router router = new Router(resourceDir);
        router.post("/api/echo", request -> {
            String payload = request.getPayload();
            if (payload == null || payload.isBlank()) {
                return new Response(Status.BAD_REQUEST, "application/json", "{\"error\": \"Bad Request\"}\n".getBytes(), request.getMethod());
            }
            return new Response(Status.CREATED, "text/plain", payload.getBytes(), request.getMethod());
        });

        Server server = new Server(port, router);
        server.start();
    }
}