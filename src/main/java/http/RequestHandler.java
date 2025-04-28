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

            if (!handleRequest(path, headers, out)) {
                out.write(HttpStatusLines.NOT_FOUND.getBytes());
            }

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

    private boolean handleRequest(String path, Map<String, String> headers, OutputStream out) throws IOException {
        if ("/".equals(path)) {
            out.write(HttpStatusLines.OK.getBytes());
            return true;
        } else if (path.startsWith("/echo/")) {
            String echoContent = path.substring("/echo/".length());
            writeTextResponse(out, echoContent);
            return true;
        } else if ("/user-agent".equals(path)) {
            String userAgent = headers.getOrDefault("user-agent", "");
            writeTextResponse(out, userAgent);
            return true;
        } else if (path.startsWith("/files/")) {
            String filename = path.substring("/files/".length());
            Path filePath = Path.of(directory, filename);

            if (Files.exists(filePath)) {
                byte[] fileContent = Files.readAllBytes(filePath);
                writeFileResponse(out, fileContent);
                return true;
            } else {
                out.write(HttpStatusLines.NOT_FOUND.getBytes());
                return true;
            }
        }

        return false;
    }

    private void writeTextResponse(OutputStream out, String body) throws IOException {
        byte[] bodyBytes = body.getBytes();

        String headers = HttpStatusLines.OK +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "\r\n";

        out.write(headers.getBytes());
        out.write(bodyBytes);
    }

    private void writeFileResponse(OutputStream out, byte[] content) throws IOException {
        String headers = HttpStatusLines.OK +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "\r\n";

        out.write(headers.getBytes());
        out.write(content);
    }
}
