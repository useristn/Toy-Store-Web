package t4m.toy_store.rating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.rating.dto.AdminRatingResponse;
import t4m.toy_store.rating.dto.AdminRatingStatsResponse;
import t4m.toy_store.rating.entity.Rating;
import t4m.toy_store.rating.repository.RatingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRatingService {
    
    private final RatingRepository ratingRepository;
    private final RatingService ratingService;
    
    /**
     * Get all ratings with filters and pagination
     */
    public Page<AdminRatingResponse> getAllRatings(
            String userName,
            String productName,
            Integer minStars,
            Integer maxStars,
            LocalDateTime startDate,
            LocalDateTime endDate,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Rating> ratings = ratingRepository.findAllWithFilters(
            userName, productName, minStars, maxStars, startDate, endDate, pageable
        );
        
        return ratings.map(this::toAdminResponse);
    }
    
    /**
     * Get rating statistics for admin dashboard
     */
    public AdminRatingStatsResponse getStatistics() {
        long total = ratingRepository.count();
        Double avgRating = ratingRepository.findAverageRatingForAll();
        
        return AdminRatingStatsResponse.builder()
            .totalReviews(total)
            .averageRating(avgRating != null ? avgRating : 0.0)
            .pendingReviews(0L) // For future use
            .fiveStars(ratingRepository.countByStars(5))
            .fourStars(ratingRepository.countByStars(4))
            .threeStars(ratingRepository.countByStars(3))
            .twoStars(ratingRepository.countByStars(2))
            .oneStar(ratingRepository.countByStars(1))
            .build();
    }
    
    /**
     * Delete a single rating
     */
    @Transactional
    public void deleteRating(Long ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
            .orElseThrow(() -> new IllegalArgumentException("Rating not found"));
        
        Long productId = rating.getProduct().getId();
        
        ratingRepository.delete(rating);
        
        // Update product rating statistics
        ratingService.updateProductRatingStats(productId);
        
        log.info("Admin deleted rating {}", ratingId);
    }
    
    /**
     * Delete multiple ratings (bulk delete)
     */
    @Transactional
    public void deleteRatings(List<Long> ratingIds) {
        List<Rating> ratings = ratingRepository.findAllById(ratingIds);
        
        if (ratings.isEmpty()) {
            throw new IllegalArgumentException("No ratings found");
        }
        
        // Collect unique product IDs to update stats later
        List<Long> productIds = ratings.stream()
            .map(r -> r.getProduct().getId())
            .distinct()
            .collect(Collectors.toList());
        
        ratingRepository.deleteAll(ratings);
        
        // Update product rating statistics for all affected products
        productIds.forEach(ratingService::updateProductRatingStats);
        
        log.info("Admin deleted {} ratings", ratings.size());
    }
    
    /**
     * Convert Rating entity to AdminRatingResponse DTO
     */
    private AdminRatingResponse toAdminResponse(Rating rating) {
        return AdminRatingResponse.builder()
            .id(rating.getId())
            .productId(rating.getProduct().getId())
            .productName(rating.getProduct().getName())
            .productImage(rating.getProduct().getImageUrl())
            .userId(rating.getUser().getId())
            .userName(rating.getUser().getName())
            .userEmail(rating.getUser().getEmail())
            .stars(rating.getStars())
            .createdAt(rating.getCreatedAt())
            .orderId(rating.getOrder().getId())
            .build();
    }
}
