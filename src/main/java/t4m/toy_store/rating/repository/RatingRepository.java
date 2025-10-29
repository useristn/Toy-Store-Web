package t4m.toy_store.rating.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import t4m.toy_store.rating.entity.Rating;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {
    
    // Check if user already rated a product in a specific order
    boolean existsByOrderIdAndProductId(Long orderId, Long productId);
    
    // Get rating by order and product
    Optional<Rating> findByOrderIdAndProductId(Long orderId, Long productId);
    
    // Calculate average rating for a product
    @Query("SELECT AVG(r.stars) FROM Rating r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
    
    // Count ratings for a product
    long countByProductId(Long productId);
    
    // Admin: Get all ratings with filtering
    @Query("SELECT r FROM Rating r " +
           "WHERE (:userName IS NULL OR LOWER(r.user.name) LIKE LOWER(CONCAT('%', :userName, '%'))) " +
           "AND (:productName IS NULL OR LOWER(r.product.name) LIKE LOWER(CONCAT('%', :productName, '%'))) " +
           "AND (:minStars IS NULL OR r.stars >= :minStars) " +
           "AND (:maxStars IS NULL OR r.stars <= :maxStars) " +
           "AND (:startDate IS NULL OR r.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR r.createdAt <= :endDate)")
    Page<Rating> findAllWithFilters(
        @Param("userName") String userName,
        @Param("productName") String productName,
        @Param("minStars") Integer minStars,
        @Param("maxStars") Integer maxStars,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    // Admin: Count ratings by stars
    long countByStars(Integer stars);
    
    // Admin: Calculate average rating for all products
    @Query("SELECT AVG(r.stars) FROM Rating r")
    Double findAverageRatingForAll();
}
