import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        ServerSocket serverSocket = new ServerSocket(port);

        for (int i = 0; true; i++) {
            Socket clientSocket = serverSocket.accept();

            System.out.println("received request #"+ i); // - #1 (next req) is for favicon.ico
            // Read and Translate incoming raw HTTP request from the browser to text
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            String line;

            // Read HTTP headers until reaching an empty line
            while((line = bufferedReader.readLine()) != null) {
                if (line.isEmpty()) {
                    break;
                }

                System.out.println(line);
            }


            // TODO: Write the raw HTTP protocol and HTTP payload to send back to the client

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
            outputStream.write(formatHeader("<h1>HELLO WORLD!</h1> <h2>testing</h2>"));
            outputStream.flush();

            clientSocket.close();
        }
    }

    // CRLF helper
    static byte[] formatHeader(String headerString) {
        return (headerString +  "\r\n").getBytes();
    }
}