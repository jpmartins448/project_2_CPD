import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Iterator;
import java.util.Map;

public class SessionManager {
    private final Map<String, Session> sessions = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Random random = new Random();
    private static final long TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24h

    public Session createSession(String username) {
        lock.lock();
        try {
            String token = generateToken();
            long expiry = System.currentTimeMillis() + TOKEN_EXPIRY_MS;
            Session session = new Session(username, token, expiry);
            sessions.put(token, session);
            return session;
        } finally {
            lock.unlock();
        }
    }

    public Session getSessionByToken(String token) {
        lock.lock();
        try {
            Session s = sessions.get(token);
            if (s != null && s.getExpiry() > System.currentTimeMillis()) {
                return s;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void expireSession(String token) {
        lock.lock();
        try {
            sessions.remove(token);
        } finally {
            lock.unlock();
        }
    }

    private String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }

    public void cleanupExpiredSessions() {
        lock.lock();
        try {
            long now = System.currentTimeMillis();

            Iterator<Map.Entry<String, Session>> it = sessions.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String, Session> e = it.next();
                if (e.getValue().getExpiry() <= now) {
                    it.remove();
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
