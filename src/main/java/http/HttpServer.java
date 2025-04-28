package http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                RequestHandler handler = new RequestHandler(clientSocket);
                handler.handle();
            }
        } catch (IOException e) {
            System.out.println("Server IOException: " + e.getMessage());
        }
    }
}
