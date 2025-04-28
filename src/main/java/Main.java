import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        System.out.println("Logs from your program will appear here!");

        try {
            ServerSocket serverSocket = new ServerSocket(4221);
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try {
            OutputStream out = clientSocket.getOutputStream();

            // Send only status line and blank line after headers
            String response = "HTTP/1.1 200 OK\r\n\r\n";

            out.write(response.getBytes());
            out.flush();

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException when handling client: " + e.getMessage());
        }
    }
}
