

import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection {
    private final Socket socket;
    private final PrintWriter out;
    private final SimpleMessageQueue<String> outboundQueue;
    private volatile boolean running = true;

    public ClientConnection(Socket socket, PrintWriter out) {
        this.socket = socket;
        this.out = out;
        this.outboundQueue = new SimpleMessageQueue<>();
        startSenderThread();
    }

    private void startSenderThread() {
        Thread.startVirtualThread(() -> {
            try {
                while (running) {
                    String msg = outboundQueue.take();
                    out.println(msg);
                }
            } catch (InterruptedException e) {
                // Thread interrupted, exit
            } finally {
                out.close();
            }
        });
    }

    public void send(String msg) {
        outboundQueue.put(msg);
    }

    public void close() {
        running = false;
        outboundQueue.put("");
        try { socket.close(); } catch (Exception ignored) {}
    }
}
