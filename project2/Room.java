import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Room {

    private static final int MAX_TIMELINE_MESSAGES = 500;

    protected final String name;
    protected final List<Message> timeline = new ArrayList<>();
    protected final Set<Session> members = new HashSet<>();
    protected final ReentrantLock lock = new ReentrantLock();
    private long nextMessageId = 1;

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void join(Session session) {
        List<Session> snapshot;
        Message sysMsg = new Message(
            "SYSTEM",
            "[" + session.getUsername() + " enters the room]",
            Message.Type.SYSTEM
        );

        lock.lock();
        try {
            members.add(session);
            addToTimeline(sysMsg);
            snapshot = List.copyOf(members);
        } finally {
            lock.unlock();
        }

        broadcastSystem(sysMsg, snapshot);
    }

    public void resume(Session session) {
        lock.lock();
        try {
            members.add(session);
        } finally {
            lock.unlock();
        }
    }

    public void leave(Session session) {
        List<Session> snapshot;
        Message sysMsg = new Message(
            "SYSTEM",
            "[" + session.getUsername() + " leaves the room]",
            Message.Type.SYSTEM
        );

        lock.lock();
        try {
            members.remove(session);
            addToTimeline(sysMsg);
            snapshot = new ArrayList<>(members);
        } finally {
            lock.unlock();
        }

        broadcastSystem(sysMsg, snapshot);
    }

    public void postMessage(Message msg) {
        List<Session> snapshot;

        lock.lock();
        try {
            addToTimeline(msg);
            snapshot = new ArrayList<>(members);
        } finally {
            lock.unlock();
        }

        broadcast(msg, snapshot);
    }

    public List<Message> getTimeline() {
        lock.lock();
        try {
            return new ArrayList<>(timeline);
        } finally {
            lock.unlock();
        }
    }

    public List<Message> getMessagesAfter(long lastSeenMessageId) {
        lock.lock();
        try {
            List<Message> messages = new ArrayList<>();
            for (Message message : timeline) {
                if (message.getId() > lastSeenMessageId) {
                    messages.add(message);
                }
            }
            return messages;
        } finally {
            lock.unlock();
        }
    }

    protected void broadcast(Message msg, List<Session> snapshot) {
        for (Session s : snapshot) {
            s.deliver(msg, name);
        }
    }

    protected void broadcastSystem(Message msg, List<Session> snapshot) {
        for (Session s : snapshot) {
            s.deliverSystem(msg, name);
        }
    }

    public void forceRemove(Session session) {
        lock.lock();
        try {
            members.remove(session);
        } finally {
            lock.unlock();
        }
    }

    private void addToTimeline(Message msg) {
        msg.setId(nextMessageId++);
        timeline.add(msg);
        while (timeline.size() > MAX_TIMELINE_MESSAGES) {
            timeline.remove(0);
        }
    }
}
