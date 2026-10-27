package com.github.mksafe.http;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String resourceDir = "public";

        Router router = new Router(resourceDir);
        router.get("/api/users", req -> {
            String page = req.queryParam("page");
            String limit = req.queryParam("limit");

            String responseBody = "{\"page\":" + (page != null ? page : "1") + "}";
            return Response.json(Status.OK, responseBody, req.method());
        });

        router.post("/api/echo", request -> {
            byte[] payload = request.payload();
            if (payload == null || new String(payload, StandardCharsets.UTF_8).isBlank()) {
                return Response.json(Status.BAD_REQUEST, "Bad Request", request.method());
            }
            return Response.bytes(Status.CREATED, payload, request.method());
        });

        Server server = new Server(port, router);
        server.start();
    }
}
