package project2;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class UserStore {
    private final Map<String, String> userToHash = new HashMap<>();
    private final Map<String, String> userToSalt = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Path file = Paths.get("users.db");

    public UserStore() {
        load();
    }

    public boolean register(String username, String password) {
        lock.lock();
        try {
            if (userToHash.containsKey(username)) return false;
            String salt = generateSalt();
            String hash = hash(password, salt);
            userToHash.put(username, hash);
            userToSalt.put(username, salt);
            save();
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean authenticate(String username, String password) {
        lock.lock();
        try {
            String hash = userToHash.get(username);
            String salt = userToSalt.get(username);
            if (hash == null || salt == null) return false;
            return hash.equals(hash(password, salt));
        } finally {
            lock.unlock();
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 3) {
                    userToHash.put(parts[0], parts[1]);
                    userToSalt.put(parts[0], parts[2]);
                }
            }
        } catch (IOException ignored) {}
    }

    private void save() {
        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            for (String user : userToHash.keySet()) {
                bw.write(user + ":" + userToHash.get(user) + ":" + userToSalt.get(user));
                bw.newLine();
            }
        } catch (IOException ignored) {}
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashed = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
