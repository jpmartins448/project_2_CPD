import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class RoomManager {
    private final Map<String, Room> rooms = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final LLMClient llm = new LLMClient("llama3.2:3b");

    public RoomManager() {
        rooms.put("Lobby", new Room("Lobby"));
    }

    public Room createAIRoom(String name, String prompt) {
        lock.lock();
        try {
            if (rooms.containsKey(name)) {
                return rooms.get(name); // evita overwrite bug
            }

            AIRoom room = new AIRoom(name, prompt, llm);
            rooms.put(name, room);
            return room;

        } finally {
            lock.unlock();
        }
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