package t4m.toy_store.favorite.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import t4m.toy_store.favorite.entity.Favorite;
import t4m.toy_store.product.dto.ProductResponse;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteResponse {
    private Long id;
    private ProductResponse product;
    private LocalDateTime createdAt;

    public static FavoriteResponse fromEntity(Favorite favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .product(ProductResponse.fromEntity(favorite.getProduct()))
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
