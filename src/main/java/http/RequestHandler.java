package http;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class RequestHandler {
    private final Socket clientSocket;
    private final String directory;

    public RequestHandler(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    public void handle() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream out = clientSocket.getOutputStream()
        ) {
            String requestLine = in.readLine();
            System.out.println("Request Line: " + requestLine);

            if (requestLine == null || requestLine.isEmpty()) {
                clientSocket.close();
                return;
            }

            String path = extractPathFromRequestLine(requestLine);
            Map<String, String> headers = readHeaders(in);

            String response = buildResponse(path, headers);

            out.write(response.getBytes());
            out.flush();
        } catch (IOException e) {
            System.out.println("Handler IOException: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private String extractPathFromRequestLine(String requestLine) {
        String[] parts = requestLine.split(" ");
        if (parts.length >= 2) {
            return parts[1];
        } else {
            return "";
        }
    }

    private Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();

        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            int colonIndex = line.indexOf(":");
            if (colonIndex != -1) {
                String headerName = line.substring(0, colonIndex).trim().toLowerCase();
                String headerValue = line.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
            }
        }

        return headers;
    }

    private String buildResponse(String path, Map<String, String> headers) {
        if ("/".equals(path)) {
            return "HTTP/1.1 200 OK\r\n\r\n";
        } else if (path.startsWith("/echo/")) {
            String echoContent = path.substring("/echo/".length());
            return buildOkResponse(echoContent);
        } else if ("/user-agent".equals(path)) {
            String userAgent = headers.getOrDefault("user-agent", "");
            return buildOkResponse(userAgent);
        } else if (path.startsWith("/files/")) {
            String filename = path.substring("/files/".length());
            Path filePath = Path.of(directory, filename);

            if (Files.exists(filePath)) {
                try {
                    byte[] fileContent = Files.readAllBytes(filePath);
                    return buildFileResponse(fileContent);
                } catch (IOException e) {
                    return "HTTP/1.1 500 Internal Server Error\r\n\r\n";
                }
            } else {
                return "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        }
        else {
            return "HTTP/1.1 404 Not Found\r\n\r\n";
        }
    }

    private String buildFileResponse(byte[] content) {
        String headers = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "\r\n";
        return headers + new String(content);
    }

    private String buildOkResponse(String body) {
        int contentLength = body.getBytes().length;
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + contentLength + "\r\n" +
                "\r\n" +
                body;
    }
}
