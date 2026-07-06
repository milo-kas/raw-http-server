import java.io.BufferedReader;
import java.io.IOException;

public class HttpRequest {

    enum HttpMethod {
        GET, POST, UNKNOWN;
    }

    public HttpRequest(BufferedReader bufferedReader) throws IOException {
        parseRequest(bufferedReader);
    }

    private void parseRequest(BufferedReader bufferedReader) throws IOException {

        // Get the actual request (first line)
        String line = bufferedReader.readLine();

        // Parse actual request
        String[] request = line.split(" ");

        String method = request[0];

        // Read HTTP headers until reaching an empty line
        while((line = bufferedReader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }

            System.out.println(line);
        }
    }
}
