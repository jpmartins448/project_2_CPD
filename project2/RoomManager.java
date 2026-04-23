package project2;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class RoomManager {
    private final Map<String, Room> rooms = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public RoomManager() {
        // Optionally create a default room
        rooms.put("Lobby", new Room("Lobby"));
    }

    public Room getOrCreateRoom(String name) {
        lock.lock();
        try {
            return rooms.computeIfAbsent(name, Room::new);
        } finally {
            lock.unlock();
        }
    }

    public Room getRoom(String name) {
        lock.lock();
        try {
            return rooms.get(name);
        } finally {
            lock.unlock();
        }
    }

    public List<String> listRooms() {
        lock.lock();
        try {
            return new ArrayList<>(rooms.keySet());
        } finally {
            lock.unlock();
        }
    }
}
