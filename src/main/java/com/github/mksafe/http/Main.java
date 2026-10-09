package com.github.mksafe.http;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String resourceDir = "public";

        Router router = new Router(resourceDir);
        router.post("/api/echo", request -> {
            String payload = request.payload();
            if (payload == null || payload.isBlank()) {
                return Response.json(Status.BAD_REQUEST, "{\"error\": \"Bad Request\"}\n", request.method());
            }
            return Response.text(Status.CREATED, payload, request.method());
        });

        Server server = new Server(port, router);
        server.start();
    }
}
