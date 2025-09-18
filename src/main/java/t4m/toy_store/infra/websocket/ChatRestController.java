package t4m.toy_store.infra.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody ChatMessage message) {
        chatService.sendMessage(message);
        return ResponseEntity.ok("Message sent to WebSocket subscribers");
    }

    @PostMapping("/notify")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationMessage notification) {
        chatService.sendNotification(notification);
        return ResponseEntity.ok("Notification sent");
    }
}
