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

    public String ask(String systemPrompt, List<Message> context) {
        try {
            StringBuilder prompt = new StringBuilder();

            prompt.append("SYSTEM: ").append(systemPrompt).append("\n");
            prompt.append("You are a helpful chat assistant. Reply briefly.\n\n");

            int start = Math.max(0, context.size() - 10);

            for (int i = start; i < context.size(); i++) {
                Message m = context.get(i);
                prompt.append(m.getAuthor())
                        .append(": ")
                        .append(m.getText())
                        .append("\n");
            }

            String json = "{"
                    + "\"model\":\"" + model + "\","
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
                .replace("\n", "\\n");
    }

    private String extractResponse(String json) {
        String key = "\"response\":\"";
        int start = json.indexOf(key);
        if (start == -1) return json;

        start += key.length();
        int end = json.indexOf("\"", start);

        if (end == -1) return json.substring(start);

        return json.substring(start, end)
                .replace("\\n", "\n")
                .replace("\\\"", "\"");
    }
}