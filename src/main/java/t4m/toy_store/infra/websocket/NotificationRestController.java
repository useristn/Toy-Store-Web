package t4m.toy_store.infra.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @PostMapping("/broadcast")
    public ResponseEntity<String> broadcast(@RequestParam String type, @RequestParam String message) {
        notificationService.sendNotification(type, message);
        return ResponseEntity.ok("Notification broadcasted");
    }
}
