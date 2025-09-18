package t4m.toy_store.infra.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(String type, String message) {
        NotificationPayload payload = new NotificationPayload(type, message, Instant.now());
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }
}
