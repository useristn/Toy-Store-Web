package t4m.toy_store.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockStats {
    private Long totalProducts;
    private Long inStockProducts;
    private Long outOfStockProducts;
    private Long lowStockProducts;
    private Long totalStockQuantity;
}
