package t4m.toy_store.support.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private String sessionId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String senderType; // USER or ADMIN
    private String message;
    private LocalDateTime createdAt;
    private String messageType; // CHAT, JOIN, LEAVE, TYPING
}
