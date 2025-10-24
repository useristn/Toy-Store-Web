package t4m.toy_store.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingResponse {
    private Long id;
    private Long productId;
    private Long userId;
    private Long orderId;
    private Integer stars;
    private String createdAt;
}
