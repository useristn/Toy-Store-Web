package t4m.toy_store.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {
    private String message;
    private String conversationId; // optional for tracking conversations
}
