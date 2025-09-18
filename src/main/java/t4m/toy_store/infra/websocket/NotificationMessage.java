package t4m.toy_store.infra.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {
    private String userId;      // null = broadcast all
    private String title;
    private String content;
}
