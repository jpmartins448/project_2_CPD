import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class SessionManager {
    private final Map<String, Session> sessionsByToken = new HashMap<>();
    private final Map<String, Session> sessionsByUser = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private static final long TOKEN_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24h

    public Session createSession(String username) {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            Session existing = sessionsByUser.get(username);
            if (existing != null && existing.getExpiry() > now) {
                return existing;
            }

            if (existing != null) {
                sessionsByToken.remove(existing.getToken());
                sessionsByUser.remove(username);
            }

            String token = generateToken();
            long expiry = now + TOKEN_EXPIRY_MS;
            Session session = new Session(username, token, expiry);
            sessionsByToken.put(token, session);
            sessionsByUser.put(username, session);
            return session;
        } finally {
            lock.unlock();
        }
    }

    public Session getSessionByToken(String token) {
        lock.lock();
        try {
            Session session = sessionsByToken.get(token);
            if (session != null && session.getExpiry() > System.currentTimeMillis()) {
                return session;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public Session expireSession(String token) {
        lock.lock();
        try {
            Session removed = sessionsByToken.remove(token);
            if (removed != null) {
                Session current = sessionsByUser.get(removed.getUsername());
                if (current == removed) {
                    sessionsByUser.remove(removed.getUsername());
                }
            }
            return removed;
        } finally {
            lock.unlock();
        }
    }

    private String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }

    public List<Session> cleanupExpiredSessions() {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            List<Session> expired = new ArrayList<>();
            Iterator<Map.Entry<String, Session>> it = sessionsByToken.entrySet().iterator();

            while (it.hasNext()) {
                Map.Entry<String, Session> entry = it.next();
                Session session = entry.getValue();
                if (session.getExpiry() <= now) {
                    it.remove();
                    Session current = sessionsByUser.get(session.getUsername());
                    if (current == session) {
                        sessionsByUser.remove(session.getUsername());
                    }
                    expired.add(session);
                }
            }

            return expired;
        } finally {
            lock.unlock();
        }
    }
}
