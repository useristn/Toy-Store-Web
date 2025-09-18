package t4m.toy_store.infra.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationPayload {
    private String type;      // ORDER_NEW, ORDER_STATUS, PROMO
    private String message;   // Nội dung thông báo
    private Instant timestamp;
}
