package t4m.toy_store.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.rating.dto.AdminRatingResponse;
import t4m.toy_store.rating.dto.AdminRatingStatsResponse;
import t4m.toy_store.rating.service.AdminRatingService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ratings")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminRatingController {
    
    private final AdminRatingService adminRatingService;
    
    /**
     * Get all ratings with filters and pagination
     */
    @GetMapping
    public ResponseEntity<Page<AdminRatingResponse>> getAllRatings(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer minStars,
            @RequestParam(required = false) Integer maxStars,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("Admin fetching ratings with filters - page: {}, size: {}", page, size);
        
        Page<AdminRatingResponse> ratings = adminRatingService.getAllRatings(
            userName, productName, minStars, maxStars, startDate, endDate,
            page, size, sortBy, sortDirection
        );
        
        return ResponseEntity.ok(ratings);
    }
    
    /**
     * Get rating statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<AdminRatingStatsResponse> getStatistics() {
        log.info("Admin fetching rating statistics");
        AdminRatingStatsResponse stats = adminRatingService.getStatistics();
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Delete a single rating
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRating(@PathVariable Long id) {
        log.info("Admin deleting rating {}", id);
        
        try {
            adminRatingService.deleteRating(id);
            return ResponseEntity.ok(Map.of("message", "Xóa đánh giá thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Bulk delete ratings
     */
    @DeleteMapping("/bulk")
    public ResponseEntity<Map<String, String>> bulkDeleteRatings(@RequestBody List<Long> ratingIds) {
        log.info("Admin bulk deleting {} ratings", ratingIds.size());
        
        try {
            adminRatingService.deleteRatings(ratingIds);
            return ResponseEntity.ok(Map.of(
                "message", "Xóa " + ratingIds.size() + " đánh giá thành công"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
