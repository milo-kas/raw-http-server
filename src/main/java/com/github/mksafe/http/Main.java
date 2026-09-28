package com.github.mksafe.http;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String resourceDir = "public";

        Router router = new Router(resourceDir);
        router.post("/api/echo", request -> {
            String payload = request.getPayload();
            if (payload == null || payload.isBlank()) {
                return Response.json(Status.BAD_REQUEST, "{\"error\": \"Bad Request\"}\n", request.getMethod());
            }
            return Response.text(Status.CREATED, payload, request.getMethod());
        });

        Server server = new Server(port, router);
        server.start();
    }
}