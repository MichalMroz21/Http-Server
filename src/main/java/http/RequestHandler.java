package http;

import java.io.*;
import java.net.Socket;
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

            String response;

            if ("GET".equals(method)) {
                response = handleGet(path, headers);
                out.write(response.getBytes());
            } else if ("POST".equals(method)) {
                response = handlePost(path, headers, in);
                out.write(response.getBytes());
            } else {
                response = HttpStatusLines.NOT_FOUND;
                out.write(response.getBytes());
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

    private String handleGet(String path, Map<String, String> headers) {
        boolean clientAcceptsGzip = false;

        if (headers.containsKey("accept-encoding")) {
            String acceptEncoding = headers.get("accept-encoding");

            if (acceptEncoding.contains("gzip")) {
                clientAcceptsGzip = true;
            }
        }

        if ("/".equals(path)) {
            return HttpStatusLines.OK + "\r\n";
        } else if (path.startsWith("/echo/")) {
            String echoContent = path.substring("/echo/".length());
            return buildOkResponse(echoContent, clientAcceptsGzip);
        } else if ("/user-agent".equals(path)) {
            String userAgent = headers.getOrDefault("user-agent", "");
            return buildOkResponse(userAgent, clientAcceptsGzip);
        } else if (path.startsWith("/files/")) {
            String filename = path.substring("/files/".length());
            Path filePath = Path.of(directory, filename);

            if (Files.exists(filePath)) {
                try {
                    byte[] fileContent = Files.readAllBytes(filePath);
                    return buildFileResponse(fileContent);
                } catch (IOException e) {
                    return HttpStatusLines.INTERNAL_SERVER_ERROR;
                }
            } else {
                return HttpStatusLines.NOT_FOUND;
            }

        } else {
            return HttpStatusLines.NOT_FOUND;
        }
    }

    private String handlePost(String path, Map<String, String> headers, BufferedReader in) {
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

                    return "HTTP/1.1 201 Created\r\n\r\n";
                } catch (IOException e) {
                    return HttpStatusLines.INTERNAL_SERVER_ERROR;
                }
            } else {
                return HttpStatusLines.INTERNAL_SERVER_ERROR;
            }
        } else {
            return HttpStatusLines.NOT_FOUND;
        }
    }

    private String buildFileResponse(byte[] content) {
        String headers = HttpStatusLines.OK +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: " + content.length + "\r\n" +
                "\r\n";
        return headers + new String(content);
    }

    private String buildOkResponse(String body, boolean clientAcceptsGzip) {
        StringBuilder response = new StringBuilder();
        response.append(HttpStatusLines.OK);
        response.append("Content-Type: text/plain\r\n");

        byte[] bodyBytes;

        try {
            if (clientAcceptsGzip) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                    gzipStream.write(body.getBytes());
                }
                bodyBytes = byteStream.toByteArray();
                response.append("Content-Encoding: gzip\r\n");
            } else {
                bodyBytes = body.getBytes();
            }

            response.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            response.append("\r\n");

            return response.toString() + new String(bodyBytes, "ISO-8859-1");

        } catch (IOException e) {
            return HttpStatusLines.INTERNAL_SERVER_ERROR;
        }
    }

}
