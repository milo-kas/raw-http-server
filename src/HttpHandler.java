import java.io.IOException;

public class HttpHandler {

    public void handleResponse(HttpRequest request, HttpResponse response) throws IOException {
        // TODO: path, payload; POST, UNKNOWN, different status
        switch (request.getMethod()) {
            case GET -> response.respond();
            case POST -> System.out.println("TODO");
            case UNKNOWN -> System.out.println("UNKNOWN");
        }
    }
}
