package http;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

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

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                clientSocket.close();
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            Map<String, String> headers = readHeaders(in);

            if ("GET".equals(method)) {
                handleGet(path, headers, out);
            } else if ("POST".equals(method)) {
                handlePost(path, headers, in, out);
            } else {
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

    private void handleGet(String path, Map<String, String> headers, OutputStream out) throws IOException {
        boolean clientAcceptsGzip = false;

        if (headers.containsKey("accept-encoding")) {
            String acceptEncoding = headers.get("accept-encoding");
            if (acceptEncoding.contains("gzip")) {
                clientAcceptsGzip = true;
            }
        }

        if ("/".equals(path)) {
            out.write((HttpStatusLines.OK + "\r\n").getBytes());
        } else if (path.startsWith("/echo/")) {
            String echoContent = path.substring("/echo/".length());
            sendOkResponse(echoContent, clientAcceptsGzip, out);
        } else if ("/user-agent".equals(path)) {
            String userAgent = headers.getOrDefault("user-agent", "");
            sendOkResponse(userAgent, clientAcceptsGzip, out);
        } else if (path.startsWith("/files/")) {
            String filename = path.substring("/files/".length());
            Path filePath = Path.of(directory, filename);

            if (Files.exists(filePath)) {
                try {
                    byte[] fileContent = Files.readAllBytes(filePath);
                    sendFileResponse(fileContent, out);
                } catch (IOException e) {
                    out.write(HttpStatusLines.INTERNAL_SERVER_ERROR.getBytes());
                }
            } else {
                out.write(HttpStatusLines.NOT_FOUND.getBytes());
            }
        } else {
            out.write(HttpStatusLines.NOT_FOUND.getBytes());
        }
    }

    private void handlePost(String path, Map<String, String> headers, BufferedReader in, OutputStream out) throws IOException {
        if (path.startsWith("/files/")) {
            String filename = path.substring("/files/".length());
            Path filePath = Path.of(directory, filename);

            int contentLength = Integer.parseInt(headers.getOrDefault("content-length", "0"));
            if (contentLength > 0) {
                try {
                    char[] bodyChars = new char[contentLength];
                    int read = in.read(bodyChars);
                    String body = new String(bodyChars, 0, read);

                    Files.writeString(filePath, body);

                    out.write("HTTP/1.1 201 Created\r\n\r\n".getBytes());
                } catch (IOException e) {
                    out.write(HttpStatusLines.INTERNAL_SERVER_ERROR.getBytes());
                }
            } else {
                out.write(HttpStatusLines.INTERNAL_SERVER_ERROR.getBytes());
            }
        } else {
            out.write(HttpStatusLines.NOT_FOUND.getBytes());
        }
    }

    private void sendFileResponse(byte[] content, OutputStream out) throws IOException {
        String headers = HttpStatusLines.OK +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "\r\n";
        out.write(headers.getBytes());
        out.write(content);
    }

    private void sendOkResponse(String body, boolean useGzip, OutputStream out) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 200 OK\r\n");
        headerBuilder.append("Content-Type: text/plain\r\n");

        byte[] bodyBytes;

        if (useGzip) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                gzipStream.write(body.getBytes(StandardCharsets.UTF_8));
            }
            bodyBytes = byteStream.toByteArray();
            headerBuilder.append("Content-Encoding: gzip\r\n");
        } else {
            bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        }

        headerBuilder.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        headerBuilder.append("\r\n");

        out.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
        out.write(bodyBytes);
    }
}