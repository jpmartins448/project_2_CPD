

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

        Thread.startVirtualThread(() -> {
            try {
                String response;
                while ((response = in.readLine()) != null) {
                    if (response.startsWith("DELIVER|")) {
                        String[] parts = response.split("\\|", 5);
                        if (parts.length == 5) {
                            System.out.printf("[%s] %s: %s\n", parts[1], parts[2], parts[4]);
                        } else {
                            System.out.println(response);
                        }
                    } else if (response.startsWith("SYSTEM|")) {
                        String[] parts = response.split("\\|", 3);
                        if (parts.length == 3) {
                            System.out.printf("[%s] * %s *\n", parts[1], parts[2]);
                        } else {
                            System.out.println(response);
                        }
                    } else if (response.startsWith("ROOMS ")) {
                        System.out.println("Available rooms: " + response.substring(6));
                    } else if (response.startsWith("OK ")) {
                        System.out.println("[OK] " + response.substring(3));
                    } else if (response.startsWith("ERR ")) {
                        System.out.println("[ERROR] " + response.substring(4));
                    } else if (response.startsWith("TOKEN ")) {
                        System.out.println("[TOKEN] " + response.substring(6));
                    } else {
                        System.out.println(response);
                    }
                }
                System.out.println("Servidor desconectado.");
            } catch (Exception e) {
                System.out.println("Conexão fechada.");
            }
        });

        System.out.println("Bem-vindo ao chat!");
        System.out.println("Escreva HELP para ver os comandos disponíveis.");

        String userInput;
        while ((userInput = keyboard.readLine()) != null) {
            out.println(userInput);
        }

        socket.close();
    }
}