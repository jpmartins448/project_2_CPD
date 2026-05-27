import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Session {

    private final String username;
    private final String token;
    private final long expiry;

    private final AtomicReference<ClientConnection> connection = new AtomicReference<>();

    private final ReentrantLock lock = new ReentrantLock();
    private String currentRoom;

    public Session(String username, String token, long expiry) {
        this.username = username;
        this.token = token;
        this.expiry = expiry;
    }

    public void bindConnection(ClientConnection conn) {
        ClientConnection old = connection.getAndSet(conn);
        if (old != null) {
            old.close();
        }
    }

    public void deliver(Message msg, String room) {
        ClientConnection conn = connection.get();
        if (conn != null) {
            conn.send(
                Protocol.DELIVER + "|" +
                room + "|" +
                msg.getAuthor() + "|" +
                msg.getTimestamp() + "|" +
                msg.getText()
            );
        }
    }

    public void deliverSystem(String text, String room) {
        ClientConnection conn = connection.get();
        if (conn != null) {
            conn.send(
                Protocol.SYSTEM + "|" + room + "|" + text
            );
        }
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public long getExpiry() {
        return expiry;
    }

    public String getCurrentRoom() {
        lock.lock();
        try {
            return currentRoom;
        } finally {
            lock.unlock();
        }
    }

    public void setCurrentRoom(String room) {
        lock.lock();
        try {
            this.currentRoom = room;
        } finally {
            lock.unlock();
        }
    }
}