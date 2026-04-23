package project2;

import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 12345;
        Socket socket = new Socket(host, port);

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

        // Thread to receive messages from server
        Thread.startVirtualThread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    System.out.println(response);
                }
                System.out.println("Servidor desconectado.");
            } catch (Exception e) {
                System.out.println("Conexão fechada.");
            }
        });

        System.out.println("Bem-vindo ao chat!");
        System.out.println("Comandos: REGISTER <user> <pass>, LOGIN <user> <pass>, LIST_ROOMS, CREATE <room>, JOIN <room>, MSG <text>, LEAVE, LOGOUT");

        String userInput;
        while ((userInput = keyboard.readLine()) != null) {
            out.println(userInput);
        }

        socket.close();
    }
}