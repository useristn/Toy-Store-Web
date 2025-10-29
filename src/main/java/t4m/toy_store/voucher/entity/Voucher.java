package t4m.toy_store.voucher.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "voucher")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    // Giá trị giảm (% hoặc tiền cố định)
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    // Giảm tối đa (cho loại PERCENTAGE)
    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    // Giá trị đơn hàng tối thiểu
    @Column(name = "min_order_value", precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    // Tổng số lượng phát hành
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    // Số lượng đã sử dụng
    @Column(name = "used_quantity", nullable = false)
    private Integer usedQuantity = 0;

    // Giới hạn mỗi người dùng
    @Column(name = "limit_per_user")
    private Integer limitPerUser;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    // Áp dụng cho danh mục (comma-separated IDs)
    @Column(name = "applicable_categories", columnDefinition = "TEXT")
    private String applicableCategories;

    // Áp dụng cho sản phẩm (comma-separated IDs)
    @Column(name = "applicable_products", columnDefinition = "TEXT")
    private String applicableProducts;

    // Áp dụng cho nhóm người dùng (comma-separated roles)
    @Column(name = "applicable_user_groups", columnDefinition = "TEXT")
    private String applicableUserGroups;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Helper method to check if voucher is currently valid
    public boolean isCurrentlyValid() {
        LocalDateTime now = LocalDateTime.now();
        return active && 
               now.isAfter(startDate) && 
               now.isBefore(endDate) && 
               usedQuantity < totalQuantity;
    }

    // Helper method to check if voucher has available quantity
    public boolean hasAvailableQuantity() {
        return usedQuantity < totalQuantity;
    }
}
