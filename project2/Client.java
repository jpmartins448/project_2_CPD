import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

public class Client {
    private static volatile String sessionToken = null;
    private static final String TOKEN_FILE = ".session_token";
    private static final AtomicReference<PrintWriter> activeOut = new AtomicReference<>();

    private static void saveToken(String token) {
        try (FileWriter fw = new FileWriter(TOKEN_FILE)) {
            fw.write(token);
        } catch (IOException ignored) {}
    }

    private static String loadToken() {
        try (BufferedReader br = new BufferedReader(new FileReader(TOKEN_FILE))) {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static void startKeyboardLoop(BufferedReader keyboard) {
        Thread.startVirtualThread(() -> {
            try {
                String userInput;
                while ((userInput = keyboard.readLine()) != null) {
                    PrintWriter out = activeOut.get();
                    if (out == null) {
                        System.out.println("[INFO] Not connected. Command not sent; retry after reconnect.");
                        continue;
                    }
                    out.println(userInput);
                    if (out.checkError()) {
                        activeOut.compareAndSet(out, null);
                        System.out.println("[INFO] Connection is down. Reconnecting...");
                    }
                }
                System.exit(0);
            } catch (IOException e) {
                System.out.println("[ERROR] Failed reading keyboard: " + e.getMessage());
                System.exit(1);
            }
        });
    }

    private static void handleServerLine(String response) {
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
            sessionToken = response.substring(6).trim();
            saveToken(sessionToken);
            System.out.println("[TOKEN] " + sessionToken);
        } else {
            System.out.println(response);
        }
    }

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12345;
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

        sessionToken = loadToken();
        startKeyboardLoop(keyboard);

        System.out.println("Bem-vindo ao chat!");
        System.out.println("Escreva HELP para ver os comandos disponíveis.");

        while (true) {
            try (Socket socket = new Socket(host, port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                activeOut.set(out);

                if (sessionToken != null && !sessionToken.isBlank()) {
                    out.println("TOKEN " + sessionToken);
                }

                String response;
                while ((response = in.readLine()) != null) {
                    handleServerLine(response);
                }

                System.out.println("Servidor desconectado.");
            } catch (IOException e) {
                System.out.println("[INFO] Ligação perdida. A tentar reconectar em 2 segundos...");
            } finally {
                activeOut.set(null);
            }

            Thread.sleep(2000);
        }
    }
}
