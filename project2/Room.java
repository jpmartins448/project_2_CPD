import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Room {

    protected final String name;
    protected final List<Message> timeline = new ArrayList<>();
    protected final Set<Session> members = new HashSet<>();
    protected final ReentrantLock lock = new ReentrantLock();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void join(Session session) {
        List<Session> snapshot;
        Message sysMsg;

        lock.lock();
        try {
            members.add(session);

            sysMsg = new Message(
                "SYSTEM",
                "[" + session.getUsername() + " enters the room]",
                Message.Type.SYSTEM
            );

            timeline.add(sysMsg);

            snapshot = List.copyOf(members);
        } finally {
            lock.unlock();
        }

        broadcastSystem(sysMsg.getText(), snapshot);
    }

    public void leave(Session session) {
        List<Session> snapshot;
        Message sysMsg;

        lock.lock();
        try {
            members.remove(session);

            sysMsg = new Message(
                "SYSTEM",
                "[" + session.getUsername() + " leaves the room]",
                Message.Type.SYSTEM
            );

            timeline.add(sysMsg);

            snapshot = new ArrayList<>(members);
        } finally {
            lock.unlock();
        }

        broadcastSystem(sysMsg.getText(), snapshot);
    }

    public void postMessage(Message msg) {
        List<Session> snapshot;

        lock.lock();
        try {
            timeline.add(msg);
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

    protected void broadcast(Message msg, List<Session> snapshot) {
        for (Session s : snapshot) {
            s.deliver(msg, name);
        }
    }

    protected void broadcastSystem(String text, List<Session> snapshot) {
        for (Session s : snapshot) {
            s.deliverSystem(text, name);
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
}