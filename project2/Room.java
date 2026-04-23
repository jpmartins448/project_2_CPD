package project2;

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

    public String getName() { return name; }

    public void join(Session session) {
        lock.lock();
        try {
            members.add(session);
            Message sysMsg = new Message("SYSTEM", "[" + session.getUsername() + " enters the room]", Message.Type.SYSTEM);
            timeline.add(sysMsg);
            broadcastSystem(sysMsg.getText());
        } finally {
            lock.unlock();
        }
    }

    public void leave(Session session) {
        lock.lock();
        try {
            members.remove(session);
            Message sysMsg = new Message("SYSTEM", "[" + session.getUsername() + " leaves the room]", Message.Type.SYSTEM);
            timeline.add(sysMsg);
            broadcastSystem(sysMsg.getText());
        } finally {
            lock.unlock();
        }
    }

    public void postMessage(Message msg) {
        lock.lock();
        try {
            timeline.add(msg);
            broadcast(msg);
        } finally {
            lock.unlock();
        }
    }

    public List<Message> getTimeline() {
        lock.lock();
        try {
            return new ArrayList<>(timeline);
        } finally {
            lock.unlock();
        }
    }

    protected void broadcast(Message msg) {
        for (Session s : members) {
            s.deliver(msg, name);
        }
    }

    protected void broadcastSystem(String text) {
        for (Session s : members) {
            s.deliverSystem(text, name);
        }
    }
}
