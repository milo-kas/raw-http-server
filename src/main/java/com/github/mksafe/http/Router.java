package com.github.mksafe.http;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Router {

    private static final System.Logger log = System.getLogger(Router.class.getName());

    private final Map<RouteKey, RouteHandler> routes = new HashMap<>();
    private final ErrorHandler errorHandler;
    private final StaticHandler staticHandler;

    public Router(ErrorHandler errorHandler, StaticHandler staticHandler) {
        this.errorHandler = errorHandler;
        this.staticHandler = staticHandler;
    }

    public Router(String resourceDir, ErrorHandler errorHandler) {
        this(errorHandler, new StaticHandler(resourceDir, errorHandler));
    }

    public Router(String resourceDir) {
        this(resourceDir, new ErrorHandler(resourceDir));
    }

    public Router() {
        this("public");
    }

    public void get(String path, RouteHandler handler) {
        routes.put(new RouteKey(Method.GET, normalisePath(path)), handler);
    }

    public void post(String path, RouteHandler handler) {
        routes.put(new RouteKey(Method.POST, normalisePath(path)), handler);
    }

    public Response handleRequest(Request request) {
        Method method = request.method();

        if (method == Method.UNKNOWN) {
            return errorHandler.createErrorResponse(Status.UNKNOWN, method, request.path());
        }

        RouteKey routeKey = new RouteKey(method, normalisePath(request.path()));
        RouteHandler handler = routes.get(routeKey);

        if (handler != null) {
            try {
                return handler.handle(request);
            } catch (Exception e) {
                log.log(System.Logger.Level.ERROR,
                        "Error executing route handler for " + request.method() + " " + request.path(), e);
                return errorHandler.createErrorResponse(Status.INTERNAL_SERVER_ERROR, method, request.path());
            }
        }

        if (method == Method.GET || method == Method.HEAD) {
            return staticHandler.handleStaticResource(request);
        }

        return errorHandler.createErrorResponse(Status.NOT_FOUND, method, request.path());
    }

    private String normalisePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String normalised = path.startsWith("/") ? path : "/" + path;
        normalised = Paths.get(normalised).normalize().toString().replace('\\', '/');
        return normalised;
    }
}
