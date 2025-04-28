public class Main {
    public static void main(String[] args) {
        System.out.println("Starting server...");
        http.HttpServer server = new http.HttpServer(4221);
        server.start();
    }
}
