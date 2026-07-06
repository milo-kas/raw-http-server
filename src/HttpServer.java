import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {

    private int port = 8080;

    public HttpServer() {}

    public  HttpServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);

        for (int i = 0; true; i++) {
            Socket clientSocket = serverSocket.accept();

            System.out.println("received request #" + i); // - #1 (next req) is for favicon.ico
            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));


            HttpRequest httpRequest = new HttpRequest(bufferedReader);

            OutputStream outputStream = clientSocket.getOutputStream();

            // status
            // content type
            // CRLF
            // payload

            // Stream back a successful response (status code 200 - 299)
            outputStream.write(formatHeader("HTTP/1.1 200 OK"));
            // Stream content type
            outputStream.write(formatHeader("Content-Type: text/html"));
            // Stream empty line to signal the end of headers
            outputStream.write(formatHeader(""));
            // Send headers
            outputStream.flush();

            // Stream payload
            outputStream.write("<h1>HELLO WORLD!</h1> <h2>testing</h2>".getBytes());
            outputStream.flush();

            clientSocket.close();
        }
    }

    // CRLF helper
    private static byte[] formatHeader(String headerString) {
        return (headerString +  "\r\n").getBytes();
    }
}
