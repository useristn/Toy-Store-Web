package t4m.toy_store.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRatingSummary {
    private Long productId;
    private Double averageRating;
    private Integer ratingCount;
}
