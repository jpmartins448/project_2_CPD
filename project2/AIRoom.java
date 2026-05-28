import java.util.List;

public class AIRoom extends Room {

    private final String prompt;
    private final LLMClient llm;
    private final SimpleMessageQueue<PromptRequest> prompts = new SimpleMessageQueue<>();

    public AIRoom(String name, String prompt, LLMClient llm) {
        super(name);
        this.prompt = prompt;
        this.llm = llm;
        startPromptWorker();
    }

    public void prompt(String username, String instruction) {
        if (instruction == null || instruction.isBlank()) return;

        List<Message> context = getTimeline();
        prompts.put(new PromptRequest(username, instruction.trim(), context));
    }

    private void startPromptWorker() {
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    PromptRequest request = prompts.take();
                    String response = llm.ask(prompt, request.context(), request.username(), request.instruction());
                    Message botMsg = new Message("Bot", response, Message.Type.BOT);
                    super.postMessage(botMsg);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
    }

    private record PromptRequest(String username, String instruction, List<Message> context) {}
}
