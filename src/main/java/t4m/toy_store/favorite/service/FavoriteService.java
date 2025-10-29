package t4m.toy_store.favorite.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.favorite.entity.Favorite;
import t4m.toy_store.favorite.repository.FavoriteRepository;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public Favorite addFavorite(String userEmail, Long productId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        // Check if already exists
        if (favoriteRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new RuntimeException("Product already in favorites");
        }
        
        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .build();
        
        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(String userEmail, Long productId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        favoriteRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    public List<Favorite> getUserFavorites(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public boolean isFavorite(String userEmail, Long productId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return favoriteRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    public long countUserFavorites(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return favoriteRepository.countByUserId(user.getId());
    }

    public List<Long> getUserFavoriteProductIds(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return favoriteRepository.findProductIdsByUserId(user.getId());
    }
}
