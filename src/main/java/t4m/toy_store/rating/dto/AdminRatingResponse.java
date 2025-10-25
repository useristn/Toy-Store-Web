package t4m.toy_store.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRatingResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productImage;
    private Long userId;
    private String userName;
    private String userEmail;
    private Integer stars;
    private LocalDateTime createdAt;
    private Long orderId;
}
