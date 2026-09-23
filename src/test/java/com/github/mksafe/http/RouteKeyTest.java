package com.github.mksafe.http;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RouteKeyTest {

    @Test
    void testEqualityAndHashCode() {
        RouteKey key1 = new RouteKey(Method.GET, "/api/echo");
        RouteKey key2 = new RouteKey(Method.GET, "/api/echo");
        RouteKey keyDifferentMethod = new RouteKey(Method.POST, "/api/echo");
        RouteKey keyDifferentPath = new RouteKey(Method.GET, "/api/other");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());

        assertNotEquals(key1, keyDifferentMethod);
        assertNotEquals(key1, keyDifferentPath);
        assertNotEquals(key1, null);
        assertNotEquals(key1, new Object());
    }

    @Test
    void testMapLookup() throws Exception {
        Map<RouteKey, RouteHandler> routes = new HashMap<>();

        RouteHandler handler = request -> new Response(Status.OK, "text/plain", "Hello".getBytes(), request.getMethod());
        routes.put(new RouteKey(Method.GET, "/api/hello"), handler);

        RouteKey lookupKey = new RouteKey(Method.GET, "/api/hello");
        RouteHandler retrieved = routes.get(lookupKey);

        assertNotNull(retrieved);
        Request request = new Request(Method.GET, "/api/hello", null);
        Response response = retrieved.handle(request);
        assertEquals(Status.OK, response.getStatus());
        assertEquals("Hello", new String(response.getPayload()));

        RouteKey missingKey = new RouteKey(Method.POST, "/api/hello");
        assertNull(routes.get(missingKey));
    }

    @Test
    void testGetters() {
        RouteKey key = new RouteKey(Method.POST, "/api/items");
        assertEquals(Method.POST, key.method());
        assertEquals("/api/items", key.path());
    }
}
