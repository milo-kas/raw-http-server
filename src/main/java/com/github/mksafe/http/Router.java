package com.github.mksafe.http;

import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Router {

    private static final System.Logger log = System.getLogger(Router.class.getName());

    private final Map<RouteKey, Route> routes = new HashMap<>();
    private final ErrorHandler errorHandler;
    private final StaticHandler staticHandler;

    private record Route(RouteHandler handler, long maxBodyBytes) {
    }

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
        register(Method.GET, path, Request.DEFAULT_MAX_BODY_BYTES, handler);
    }

    public void post(String path, RouteHandler handler) {
        post(path, Request.DEFAULT_MAX_BODY_BYTES, handler);
    }

    public void post(String path, long maxBodyBytes, RouteHandler handler) {
        register(Method.POST, path, maxBodyBytes, handler);
    }

    private void register(Method method, String path, long maxBodyBytes, RouteHandler handler) {
        if (maxBodyBytes < 0) {
            throw new IllegalArgumentException("maxBodyBytes must be non-negative: " + maxBodyBytes);
        }
        routes.put(new RouteKey(method, normalisePath(path)), new Route(handler, maxBodyBytes));
    }

    public Response handleRequest(Request request) {
        Method method = request.method();

        if (method == Method.UNKNOWN) {
            return errorHandler.createErrorResponse(Status.UNKNOWN, method, request.path());
        }

        RouteKey routeKey = new RouteKey(method, normalisePath(request.path()));
        Route route = routes.get(routeKey);

        if (route != null) {
            if (request.body().length() > route.maxBodyBytes()) {
                return errorHandler.createErrorResponse(Status.PAYLOAD_TOO_LARGE, method, request.path());
            }
            try {
                return route.handler().handle(request);
            } catch (PayloadTooLargeException e) {
                return errorHandler.createErrorResponse(Status.PAYLOAD_TOO_LARGE, method, request.path());
            } catch (UncheckedIOException e) {
                if (e.getCause() instanceof PayloadTooLargeException) {
                    return errorHandler.createErrorResponse(Status.PAYLOAD_TOO_LARGE, method, request.path());
                }
                log.log(System.Logger.Level.ERROR,
                        "Error executing route handler for " + request.method() + " " + request.path(), e);
                return errorHandler.createErrorResponse(Status.INTERNAL_SERVER_ERROR, method, request.path());
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
