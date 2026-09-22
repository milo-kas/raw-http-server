package com.github.mksafe.http;

public class Handler {

    private final ErrorHandler errorHandler;
    private final StaticHandler staticHandler;

    public Handler(String resourceDir) {
        this.errorHandler = new ErrorHandler(resourceDir);
        this.staticHandler = new StaticHandler(resourceDir, errorHandler);
    }

    public Response handleRequest(Request request) {
        return switch (request.getMethod()) {
            case GET, HEAD -> staticHandler.handleStaticResource(request);
            case POST -> handlePostMethod(request);
            case UNKNOWN -> errorHandler.createErrorResponse(Status.UNKNOWN, request.getMethod(), request.getPath()); // send 501 status code
        };
    }

    private Response handlePostMethod(Request request) {
        String path = request.getPath();

        // Check for api echo test
        if (path.equals("/api/echo")) {
            String payload = request.getPayload();

            // No payload provided => bad request
            if (payload == null || payload.isBlank()) {
                return errorHandler.createErrorResponse(Status.BAD_REQUEST, request.getMethod(), request.getPath());
            }

            // Echo back the payload
            return new Response(Status.CREATED, "text/plain",  payload.getBytes(), request.getMethod());
        }

        return errorHandler.createErrorResponse(Status.NOT_FOUND, request.getMethod(), request.getPath());
    }
}
