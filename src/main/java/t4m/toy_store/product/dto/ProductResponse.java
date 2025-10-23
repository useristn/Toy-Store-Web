package t4m.toy_store.product.dto;

import lombok.*;
import t4m.toy_store.product.entity.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String imageUrl;
    private Integer stock;
    private Boolean featured;
    private CategoryInfo category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfo {
        private Long id;
        private String name;
        private String icon;
    }

    public static ProductResponse fromEntity(Product product) {
        CategoryInfo categoryInfo = null;
        if (product.getCategory() != null) {
            categoryInfo = new CategoryInfo(
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getCategory().getIcon()
            );
        }

        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .discountPrice(product.getDiscountPrice())
            .imageUrl(product.getImageUrl())
            .stock(product.getStock())
            .featured(product.getFeatured())
            .category(categoryInfo)
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}
