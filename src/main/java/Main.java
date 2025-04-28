import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            // Read the request line
            String requestLine = in.readLine();
            System.out.println("Request Line: " + requestLine);

            String path = extractPathFromRequestLine(requestLine);

            String response;
            if ("/".equals(path)) {
                response = "HTTP/1.1 200 OK\r\n\r\n";
            } else {
                response = "HTTP/1.1 404 Not Found\r\n\r\n";
            }

            out.write(response.getBytes());
            out.flush();

            clientSocket.close();
        } catch (IOException e) {
            System.out.println("IOException when handling client: " + e.getMessage());
        }
    }

    private static String extractPathFromRequestLine(String requestLine) {
        if (requestLine == null || requestLine.isEmpty()) {
            return "";
        }

        String[] parts = requestLine.split(" ");
        if (parts.length >= 2) {
            return parts[1]; // The path is the second part of the request line
        } else {
            return "";
        }
    }
}
