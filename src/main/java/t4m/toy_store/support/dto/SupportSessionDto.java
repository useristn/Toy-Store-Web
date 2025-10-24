package t4m.toy_store.support.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportSessionDto {
    private Long id;
    private String sessionId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int unreadCount;
    private String lastMessage;
}
