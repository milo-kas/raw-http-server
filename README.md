# Raw HTTP Server
> Lightweight HTTP server built in Java 21 without dependencies, allowing for high-concurrency workloads via virtual threads and memory-efficient payload streaming.

## Overview
- Virtual threads - uses `Executors.newVirtualThreadPerTaskExecutor()` for each connection.
- Supports both in-memory byte responses for small API payloads and direct socket streaming (`InputStream.transferTo()`) for constant memory overhead.
- Dynamic & Static Routing - Endpoint declarations (GET, POST) via lambda, and automatic static asset serving.
- Security - Enforces canonical path sanitisation, default security headers, RFC 9110 date headers, and caller-adaptive error formatting.

## Requirements
- Java 21+
- Maven 3.9+

## Quickstart
Simply define the routes and start the server
```java
import com.github.mksafe.http.Router;
import com.github.mksafe.http.Server;
import com.github.mksafe.http.Response;
import com.github.mksafe.http.Status;

public class Main {
    public static void main(String[] args) throws Exception {
        // Point to the root directory for static files (e.g., ./public)
        Router router = new Router("public");

        // Register functional endpoints
        router.get("/api/health", req -> Response.json(Status.OK, "{\"status\":\"UP\"}", req.getMethod()));
        router.post("/api/echo", req -> Response.text(Status.CREATED, req.getPayload(), req.getMethod()));

        // Start listening
        Server server = new Server(8080, router);
        server.start();
    }
}
```
