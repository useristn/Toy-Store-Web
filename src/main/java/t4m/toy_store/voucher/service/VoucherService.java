package t4m.toy_store.voucher.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.voucher.dto.VoucherValidationResponse;
import t4m.toy_store.voucher.entity.DiscountType;
import t4m.toy_store.voucher.entity.Voucher;
import t4m.toy_store.voucher.entity.VoucherUsage;
import t4m.toy_store.voucher.repository.VoucherRepository;
import t4m.toy_store.voucher.repository.VoucherUsageRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherService {
    
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Transactional(readOnly = true)
    public VoucherValidationResponse validateVoucher(String code, BigDecimal orderTotal, User user) {
        // Find voucher by code
        Voucher voucher = voucherRepository.findByCodeAndActiveTrue(code)
                .orElse(null);
        
        if (voucher == null) {
            return new VoucherValidationResponse(false, "Mã giảm giá không tồn tại hoặc đã bị vô hiệu hóa", null, null);
        }

        // Check date range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getStartDate())) {
            return new VoucherValidationResponse(false, "Mã giảm giá chưa đến thời gian áp dụng", null, null);
        }
        
        if (now.isAfter(voucher.getEndDate())) {
            return new VoucherValidationResponse(false, "Mã giảm giá đã hết hạn", null, null);
        }

        // Check quantity
        if (voucher.getUsedQuantity() >= voucher.getTotalQuantity()) {
            return new VoucherValidationResponse(false, "Mã giảm giá đã hết lượt sử dụng", null, null);
        }

        // Check minimum order value
        if (voucher.getMinOrderValue() != null && orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            return new VoucherValidationResponse(
                false, 
                String.format("Đơn hàng tối thiểu %,.0fđ để áp dụng mã này", voucher.getMinOrderValue()),
                null,
                null
            );
        }

        // Check limit per user
        if (voucher.getLimitPerUser() != null && user != null) {
            long userUsageCount = voucherUsageRepository.countByVoucherAndUser(voucher, user);
            if (userUsageCount >= voucher.getLimitPerUser()) {
                return new VoucherValidationResponse(
                    false,
                    "Bạn đã sử dụng hết lượt áp dụng mã giảm giá này",
                    null,
                    null
                );
            }
        }

        // Calculate discount
        BigDecimal discountAmount = calculateDiscount(voucher, orderTotal);
        
        return new VoucherValidationResponse(
            true,
            "Áp dụng mã giảm giá thành công!",
            discountAmount,
            voucher.getCode()
        );
    }

    public BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal discount = BigDecimal.ZERO;
        
        switch (voucher.getDiscountType()) {
            case PERCENTAGE:
                // Calculate percentage discount
                discount = orderTotal.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                
                // Apply max discount if specified
                if (voucher.getMaxDiscount() != null && discount.compareTo(voucher.getMaxDiscount()) > 0) {
                    discount = voucher.getMaxDiscount();
                }
                break;
                
            case FIXED_AMOUNT:
                // Fixed amount discount (cannot exceed order total)
                discount = voucher.getDiscountValue();
                if (discount.compareTo(orderTotal) > 0) {
                    discount = orderTotal;
                }
                break;
                
            case FREE_SHIPPING:
                // For free shipping, discount is the shipping fee
                // This should be calculated based on actual shipping fee
                // For now, return the discount value as max shipping discount
                discount = voucher.getDiscountValue() != null ? voucher.getDiscountValue() : BigDecimal.ZERO;
                break;
        }
        
        return discount.setScale(0, RoundingMode.HALF_UP);
    }

    @Transactional
    public void recordVoucherUsage(Voucher voucher, User user) {
        // Increment used quantity
        voucher.setUsedQuantity(voucher.getUsedQuantity() + 1);
        voucherRepository.save(voucher);
        
        // Record usage
        VoucherUsage usage = new VoucherUsage();
        usage.setVoucher(voucher);
        usage.setUser(user);
        usage.setUsedAt(LocalDateTime.now());
        voucherUsageRepository.save(usage);
    }

    @Transactional(readOnly = true)
    public Voucher getVoucherByCode(String code) {
        return voucherRepository.findByCodeAndActiveTrue(code).orElse(null);
    }
}
