package t4m.toy_store.infra.websocket;

//# Business logic (save message, broadcast)

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendMessage(ChatMessage message) {
        if (message.getRoomId() == null && message.getSenderId() != null && message.getReceiverId() != null) {
            // auto-generate roomId để consistent giữa 2 user
            String roomId = generateRoomId(message.getSenderId(), message.getReceiverId());
            message.setRoomId(roomId);
        }
        messagingTemplate.convertAndSend("/topic/chat." + message.getRoomId(), message);
    }

    public void sendNotification(NotificationMessage notification) {
        if (notification.getUserId() == null) {
            // broadcast all
            messagingTemplate.convertAndSend("/topic/notification.all", notification);
        } else {
            messagingTemplate.convertAndSend("/topic/notification." + notification.getUserId(), notification);
        }
    }

    private String generateRoomId(String senderId, String receiverId) {
        return (senderId.compareTo(receiverId) < 0)
                ? senderId + "_" + receiverId
                : receiverId + "_" + senderId;
    }
}