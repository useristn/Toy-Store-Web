package t4m.toy_store.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.favorite.dto.AddFavoriteRequest;
import t4m.toy_store.favorite.dto.FavoriteResponse;
import t4m.toy_store.favorite.entity.Favorite;
import t4m.toy_store.favorite.service.FavoriteService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    
    private final FavoriteService favoriteService;

    @PostMapping("/add")
    public ResponseEntity<?> addFavorite(
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody AddFavoriteRequest request) {
        try {
            Favorite favorite = favoriteService.addFavorite(userEmail, request.getProductId());
            return ResponseEntity.ok(FavoriteResponse.fromEntity(favorite));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<?> removeFavorite(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable Long productId) {
        try {
            favoriteService.removeFavorite(userEmail, productId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Removed from favorites");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<FavoriteResponse>> getUserFavorites(
            @RequestHeader("X-User-Email") String userEmail) {
        List<Favorite> favorites = favoriteService.getUserFavorites(userEmail);
        List<FavoriteResponse> response = favorites.stream()
                .map(FavoriteResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @RequestHeader("X-User-Email") String userEmail,
            @PathVariable Long productId) {
        boolean isFavorite = favoriteService.isFavorite(userEmail, productId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isFavorite", isFavorite);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getFavoriteCount(
            @RequestHeader("X-User-Email") String userEmail) {
        long count = favoriteService.countUserFavorites(userEmail);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/product-ids")
    public ResponseEntity<List<Long>> getFavoriteProductIds(
            @RequestHeader("X-User-Email") String userEmail) {
        List<Long> productIds = favoriteService.getUserFavoriteProductIds(userEmail);
        return ResponseEntity.ok(productIds);
    }
}
