package t4m.toy_store.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import t4m.toy_store.voucher.entity.DiscountType;
import t4m.toy_store.voucher.entity.Voucher;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderValue;
    private Integer totalQuantity;
    private Integer usedQuantity;
    private Integer limitPerUser;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean active;
    private String applicableCategories;
    private String applicableProducts;
    private String applicableUserGroups;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status; // "ACTIVE", "UPCOMING", "EXPIRED", "DISABLED"

    public static VoucherResponse fromEntity(Voucher voucher) {
        VoucherResponse response = new VoucherResponse();
        response.setId(voucher.getId());
        response.setCode(voucher.getCode());
        response.setDescription(voucher.getDescription());
        response.setDiscountType(voucher.getDiscountType());
        response.setDiscountValue(voucher.getDiscountValue());
        response.setMaxDiscount(voucher.getMaxDiscount());
        response.setMinOrderValue(voucher.getMinOrderValue());
        response.setTotalQuantity(voucher.getTotalQuantity());
        response.setUsedQuantity(voucher.getUsedQuantity());
        response.setLimitPerUser(voucher.getLimitPerUser());
        response.setStartDate(voucher.getStartDate());
        response.setEndDate(voucher.getEndDate());
        response.setActive(voucher.getActive());
        response.setApplicableCategories(voucher.getApplicableCategories());
        response.setApplicableProducts(voucher.getApplicableProducts());
        response.setApplicableUserGroups(voucher.getApplicableUserGroups());
        response.setCreatedAt(voucher.getCreatedAt());
        response.setUpdatedAt(voucher.getUpdatedAt());
        
        // Calculate status
        LocalDateTime now = LocalDateTime.now();
        if (!voucher.getActive()) {
            response.setStatus("DISABLED");
        } else if (now.isBefore(voucher.getStartDate())) {
            response.setStatus("UPCOMING");
        } else if (now.isAfter(voucher.getEndDate())) {
            response.setStatus("EXPIRED");
        } else if (voucher.getUsedQuantity() >= voucher.getTotalQuantity()) {
            response.setStatus("OUT_OF_STOCK");
        } else {
            response.setStatus("ACTIVE");
        }
        
        return response;
    }
}
