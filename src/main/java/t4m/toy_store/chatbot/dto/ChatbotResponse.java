package t4m.toy_store.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponse {
    private String reply;
    private String conversationId;
    private boolean success;
    private String error;
    
    public static ChatbotResponse success(String reply, String conversationId) {
        ChatbotResponse response = new ChatbotResponse();
        response.setReply(reply);
        response.setConversationId(conversationId);
        response.setSuccess(true);
        return response;
    }
    
    public static ChatbotResponse error(String errorMessage) {
        ChatbotResponse response = new ChatbotResponse();
        response.setSuccess(false);
        response.setError(errorMessage);
        response.setReply("Xin lỗi bạn, mình đang gặp chút trục trặc. Bạn thử lại sau nhé! 😊");
        return response;
    }
}
