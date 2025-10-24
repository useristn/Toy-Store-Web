package t4m.toy_store.rating.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.rating.dto.ProductRatingSummary;
import t4m.toy_store.rating.dto.RatingRequest;
import t4m.toy_store.rating.dto.RatingResponse;
import t4m.toy_store.rating.service.RatingService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
@Slf4j
public class RatingController {
    
    private final RatingService ratingService;
    
    @PostMapping
    public ResponseEntity<?> addRating(
        @Valid @RequestBody RatingRequest request,
        @AuthenticationPrincipal User user
    ) {
        try {
            RatingResponse response = ratingService.addRating(request, user);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Error adding rating: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Unexpected error adding rating", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "An error occurred while adding rating");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    @GetMapping("/product/{productId}/summary")
    public ResponseEntity<ProductRatingSummary> getProductRatingSummary(
        @PathVariable Long productId
    ) {
        try {
            ProductRatingSummary summary = ratingService.getProductRatingSummary(productId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/can-rate")
    public ResponseEntity<Map<String, Boolean>> canRateProduct(
        @RequestParam Long orderId,
        @RequestParam Long productId,
        @AuthenticationPrincipal User user
    ) {
        boolean canRate = ratingService.canRateProduct(orderId, productId, user);
        Map<String, Boolean> response = new HashMap<>();
        response.put("canRate", canRate);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/has-rated")
    public ResponseEntity<Map<String, Boolean>> hasRated(
        @RequestParam Long orderId,
        @RequestParam Long productId
    ) {
        boolean hasRated = ratingService.hasRated(orderId, productId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("hasRated", hasRated);
        return ResponseEntity.ok(response);
    }
}
