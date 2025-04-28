public class Main {
    public static void main(String[] args) {
        System.out.println("Starting server...");

        String directory = null;
        
        for (int i = 0; i < args.length; i++) {
            if ("--directory".equals(args[i]) && i + 1 < args.length) {
                directory = args[i + 1];
            }
        }

        if (directory == null) {
            System.out.println("No directory specified, using default directory.");
            directory = ".";
        }

        http.HttpServer server = new http.HttpServer(4221, directory);
        server.start();
    }
}
