package com.github.mksafe.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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

        long maxUploadBytes = 50L * 1024 * 1024;
        Path uploadDir = Files.createTempDirectory("raw-http-uploads");
        router.post("/api/upload", maxUploadBytes, request -> {
            Path destination = Files.createTempFile(uploadDir, "upload-", ".bin");
            long received;
            try (InputStream body = request.body().stream()) {
                received = Files.copy(body, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
                throw e;
            }

            String responseBody = "{\"file\":\"" + destination.getFileName()
                + "\",\"size\":" + received + "}";
            return Response.json(Status.CREATED, responseBody, request.method());
        });

        Server server = new Server(port, router, maxUploadBytes);
        server.start();
    }
}
