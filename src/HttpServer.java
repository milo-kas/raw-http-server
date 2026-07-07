import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    private int port = 8080;

    public HttpServer() {}

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);

        // creater handler
        HttpHandler httpHandler = new HttpHandler();

        for (int i = 0; true; i++) {
            Socket clientSocket = serverSocket.accept();

            System.out.println("--- received request #" + i); // - #1 (next req) is for favicon.ico
            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));


            HttpRequest request = new HttpRequest(bufferedReader);

            OutputStream outputStream = clientSocket.getOutputStream();

            HttpResponse response = new HttpResponse(outputStream);

            httpHandler.handleResponse(request, response);

            clientSocket.close();
        }
    }
}
