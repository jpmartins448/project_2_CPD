import java.net.URI;
import java.net.http.*;
import java.util.*;

public class LLMClient {

    private final String model;
    private final HttpClient http;

    public LLMClient(String model) {
        this.model = model;
        this.http = HttpClient.newHttpClient();
    }

    public String ask(String systemPrompt, List<Message> context, String username, String instruction) {
        try {
            StringBuilder prompt = new StringBuilder();

            prompt.append("SYSTEM: ").append(systemPrompt).append("\n");
            prompt.append("You are the Bot user inside this chat room. Use the conversation context below to answer the user's prompt. Reply briefly and directly.\n\n");
            prompt.append("Conversation so far:\n");

            for (Message m : context) {
                if (m.getType() == Message.Type.SYSTEM) continue;
                prompt.append(m.getAuthor())
                        .append(": ")
                        .append(m.getText().replace("\n", " "))
                        .append("\n");
            }

            prompt.append("\n")
                    .append(username)
                    .append(" asks Bot: ")
                    .append(instruction)
                    .append("\nBot:");

            String json = "{"
                    + "\"model\":\"" + escape(model) + "\","
                    + "\"prompt\":\"" + escape(prompt.toString()) + "\","
                    + "\"stream\":false"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    http.send(request, HttpResponse.BodyHandlers.ofString());

            return extractResponse(response.body());

        } catch (Exception e) {
            return "AI error: " + e.getMessage();
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractResponse(String json) {
        String key = "\"response\":";
        int keyStart = json.indexOf(key);
        if (keyStart == -1) return json;

        int start = json.indexOf('"', keyStart + key.length());
        if (start == -1) return json;

        StringBuilder value = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    default -> value.append(c);
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                return value.toString();
            } else {
                value.append(c);
            }
        }

        return value.toString();
    }
}
