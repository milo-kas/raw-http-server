import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        ServerSocket serverSocket = new ServerSocket(port);
        Socket clientSocket = serverSocket.accept();

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

        clientSocket.close();
    }
}