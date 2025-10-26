package t4m.toy_store.voucher.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import t4m.toy_store.voucher.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class VoucherRequest {
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher không được quá 50 ký tự")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Mã voucher chỉ được chứa chữ in hoa và số")
    private String code;

    @Size(max = 1000, message = "Mô tả không được quá 1000 ký tự")
    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.0", message = "Giảm tối đa không được âm")
    private BigDecimal maxDiscount;

    @DecimalMin(value = "0.0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @NotNull(message = "Tổng số lượng không được để trống")
    @Min(value = 1, message = "Tổng số lượng phải ít nhất là 1")
    private Integer totalQuantity;

    @Min(value = 1, message = "Giới hạn mỗi người dùng phải ít nhất là 1")
    private Integer limitPerUser;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    private Boolean active = true;

    private String applicableCategories;
    private String applicableProducts;
    private String applicableUserGroups;
}
