public enum HttpMethod {
    GET, POST, UNKNOWN;

    HttpMethod setMethod(String method) {
        try {
            return HttpMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}