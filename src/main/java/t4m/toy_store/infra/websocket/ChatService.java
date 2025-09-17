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
        messagingTemplate.convertAndSend("/topic/public", message);
    }
}
