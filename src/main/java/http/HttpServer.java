package http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class HttpServer {
    private final int port;
    private final String directory;

    public HttpServer(int port, String directory) {
        this.port = port;
        this.directory = directory;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");

                RequestHandler handler = new RequestHandler(clientSocket, directory);
                new Thread(handler::handle).start();
            }
        } catch (IOException e) {
            System.out.println("Server IOException: " + e.getMessage());
        }
    }
}
