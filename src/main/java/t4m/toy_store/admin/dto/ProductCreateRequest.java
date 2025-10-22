package t4m.toy_store.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductCreateRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String imageUrl;
    private Integer stock;
    private Long categoryId;
    private Boolean featured;
}
