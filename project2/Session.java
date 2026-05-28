import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Session {

    private final String username;
    private final String token;
    private final long expiry;

    private final AtomicReference<ClientConnection> connection = new AtomicReference<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Long> lastSeenByRoom = new HashMap<>();
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

    public void unbindConnection(ClientConnection conn) {
        connection.compareAndSet(conn, null);
    }

    public void deliver(Message msg, String room) {
        ClientConnection conn = connection.get();
        if (conn != null) {
            conn.send(
                Protocol.DELIVER + "|" +
                room + "|" +
                msg.getAuthor() + "|" +
                msg.getTimestamp() + "|" +
                sanitize(msg.getText())
            );
            markDelivered(room, msg.getId());
        }
    }

    public void deliverSystem(Message msg, String room) {
        ClientConnection conn = connection.get();
        if (conn != null) {
            conn.send(Protocol.SYSTEM + "|" + room + "|" + sanitize(msg.getText()));
            markDelivered(room, msg.getId());
        }
    }

    public void deliverHistory(Message msg, String room) {
        if (msg.getType() == Message.Type.SYSTEM) {
            deliverSystem(msg, room);
        } else {
            deliver(msg, room);
        }
    }

    public long getLastSeenMessageId(String room) {
        lock.lock();
        try {
            return lastSeenByRoom.getOrDefault(room, 0L);
        } finally {
            lock.unlock();
        }
    }

    private void markDelivered(String room, long messageId) {
        if (messageId <= 0) return;

        lock.lock();
        try {
            long current = lastSeenByRoom.getOrDefault(room, 0L);
            if (messageId > current) {
                lastSeenByRoom.put(room, messageId);
            }
        } finally {
            lock.unlock();
        }
    }

    private String sanitize(String text) {
        return text.replace("\r", " ").replace("\n", " ");
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
