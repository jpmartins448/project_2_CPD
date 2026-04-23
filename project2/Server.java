package project2;

import java.net.*;

public class Server {
    public static void main(String[] args) throws Exception {
        int port = 12345;
        ServerSocket server = new ServerSocket(port);
        System.out.println("Servidor à espera na porta " + port);

        UserStore userStore = new UserStore();
        SessionManager sessionManager = new SessionManager();
        RoomManager roomManager = new RoomManager();

        while (true) {
            Socket client = server.accept();
            System.out.println("Novo cliente conectado!");
            Thread.startVirtualThread(new ClientHandler(client, userStore, sessionManager, roomManager));
        }
    }
}