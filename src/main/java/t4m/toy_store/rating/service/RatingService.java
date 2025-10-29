package t4m.toy_store.rating.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.order.entity.OrderStatus;
import t4m.toy_store.order.repository.OrderRepository;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.ProductRepository;
import t4m.toy_store.rating.dto.ProductRatingSummary;
import t4m.toy_store.rating.dto.RatingRequest;
import t4m.toy_store.rating.dto.RatingResponse;
import t4m.toy_store.rating.entity.Rating;
import t4m.toy_store.rating.repository.RatingRepository;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {
    
    private final RatingRepository ratingRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    @Transactional
    public RatingResponse addRating(RatingRequest request, User user) {
        // Validate order exists and belongs to user
        Order order = orderRepository.findById(request.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only rate products from your own orders");
        }
        
        // Validate order is delivered
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("You can only rate products from delivered orders");
        }
        
        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        // Validate product is in the order
        boolean productInOrder = order.getOrderItems().stream()
            .anyMatch(item -> item.getProduct().getId().equals(request.getProductId()));
        
        if (!productInOrder) {
            throw new IllegalArgumentException("This product is not in the specified order");
        }
        
        // Check if already rated
        if (ratingRepository.existsByOrderIdAndProductId(request.getOrderId(), request.getProductId())) {
            throw new IllegalArgumentException("You have already rated this product for this order");
        }
        
        // Create rating
        Rating rating = Rating.builder()
            .product(product)
            .user(user)
            .order(order)
            .stars(request.getStars())
            .build();
        
        rating = ratingRepository.save(rating);
        
        // Update product rating statistics
        updateProductRatingStats(product.getId());
        
        log.info("User {} rated product {} with {} stars", user.getId(), product.getId(), request.getStars());
        
        return toResponse(rating);
    }
    
    @Transactional
    public void updateProductRatingStats(Long productId) {
        Double avgRating = ratingRepository.findAverageRatingByProductId(productId);
        long count = ratingRepository.countByProductId(productId);
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        product.setRatingCount((int) count);
        
        productRepository.save(product);
        
        log.info("Updated rating stats for product {}: avg={}, count={}", productId, avgRating, count);
    }
    
    public ProductRatingSummary getProductRatingSummary(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        
        return ProductRatingSummary.builder()
            .productId(productId)
            .averageRating(product.getAverageRating() != null ? product.getAverageRating() : 0.0)
            .ratingCount(product.getRatingCount() != null ? product.getRatingCount() : 0)
            .build();
    }
    
    public boolean canRateProduct(Long orderId, Long productId, User user) {
        // Check if order exists and belongs to user
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || !order.getUser().getId().equals(user.getId())) {
            return false;
        }
        
        // Check if order is delivered
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return false;
        }
        
        // Check if product is in order
        boolean productInOrder = order.getOrderItems().stream()
            .anyMatch(item -> item.getProduct().getId().equals(productId));
        
        if (!productInOrder) {
            return false;
        }
        
        // Check if not already rated
        return !ratingRepository.existsByOrderIdAndProductId(orderId, productId);
    }
    
    public boolean hasRated(Long orderId, Long productId) {
        return ratingRepository.existsByOrderIdAndProductId(orderId, productId);
    }
    
    private RatingResponse toResponse(Rating rating) {
        return RatingResponse.builder()
            .id(rating.getId())
            .productId(rating.getProduct().getId())
            .userId(rating.getUser().getId())
            .orderId(rating.getOrder().getId())
            .stars(rating.getStars())
            .createdAt(rating.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .build();
    }
}
