package fileserver;

public class MainServer {
    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
        try {
            Thread.sleep(50000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        server.stopServer();
    }
}
