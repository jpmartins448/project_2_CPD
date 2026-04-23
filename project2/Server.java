import java.net.*;
import java.io.*;

public class Server {

    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(12345);
        System.out.println("Servidor à espera...");

        while (true) {
            Socket client = server.accept();
            System.out.println("Novo cliente conectado!");

            //virtual thread
            Thread.startVirtualThread(() -> handleClient(client));
        }
    }

    static void handleClient(Socket client) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));

            PrintWriter out = new PrintWriter(
                    client.getOutputStream(), true);

            String msg;

            while ((msg = in.readLine()) != null) {
                System.out.println("Recebido: " + msg);
                out.println("Servidor recebeu: " + msg);
            }

            System.out.println("Cliente desconectado.");

        } catch (Exception e) {
            System.out.println("Erro na conexão com cliente.");
        }
    }
}