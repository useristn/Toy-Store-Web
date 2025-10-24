package t4m.toy_store.rating.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import t4m.toy_store.rating.entity.Rating;

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
}
