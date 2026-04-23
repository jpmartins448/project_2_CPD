package project2;

import java.time.Instant;

public class Message {
    public enum Type { USER, SYSTEM, BOT }
    private final String author;
    private final String text;
    private final Instant timestamp;
    private final Type type;

    public Message(String author, String text, Type type) {
        this.author = author;
        this.text = text;
        this.type = type;
        this.timestamp = Instant.now();
    }

    public String getAuthor() { return author; }
    public String getText() { return text; }
    public Instant getTimestamp() { return timestamp; }
    public Type getType() { return type; }
}
