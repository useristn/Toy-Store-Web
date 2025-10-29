package t4m.toy_store.rating.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRatingStatsResponse {
    private Long totalReviews;
    private Double averageRating;
    private Long pendingReviews; // For future use if we add approval status
    private Long fiveStars;
    private Long fourStars;
    private Long threeStars;
    private Long twoStars;
    private Long oneStar;
}
