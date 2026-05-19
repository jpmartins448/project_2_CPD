import java.util.List;

public class AIRoom extends Room {

    private final String prompt;
    private final LLMClient llm;

    public AIRoom(String name, String prompt, LLMClient llm) {
        super(name);
        this.prompt = prompt;
        this.llm = llm;
    }

    @Override
    public void postMessage(Message msg) {

        if (msg.getType() != Message.Type.USER) return;
        if (msg.getAuthor().equals("Bot")) return;

        super.postMessage(msg);

        List<Message> context = new ArrayList<>(getTimeline());

        Thread.startVirtualThread(() -> {
            String response = llm.ask(prompt, context);
            Message botMsg = new Message("Bot", response, Message.Type.BOT);
            super.postMessage(botMsg);
        });
    }
}