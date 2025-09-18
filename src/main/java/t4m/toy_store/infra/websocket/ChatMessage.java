package t4m.toy_store.infra.websocket;

//# DTO Entity cho message


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String senderId;   // id người gửi
    private String receiverId; // id người nhận (cho private chat)
    private String roomId;     // room (chat 1-1)
    private String content;
    private MessageType type;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        NOTIFICATION
    }
}