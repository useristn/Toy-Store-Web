package t4m.toy_store.voucher.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.voucher.dto.VoucherRequest;
import t4m.toy_store.voucher.dto.VoucherResponse;
import t4m.toy_store.voucher.dto.VoucherStatsResponse;
import t4m.toy_store.voucher.entity.DiscountType;
import t4m.toy_store.voucher.entity.Voucher;
import t4m.toy_store.voucher.repository.VoucherRepository;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AdminVoucherService {
    
    private final VoucherRepository voucherRepository;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();

    @Transactional
    public VoucherResponse createVoucher(VoucherRequest request) {
        // Check if code already exists
        if (voucherRepository.findByCode(request.getCode()).isPresent()) {
            throw new RuntimeException("Mã voucher đã tồn tại");
        }

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        Voucher voucher = new Voucher();
        voucher.setCode(request.getCode().toUpperCase());
        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setTotalQuantity(request.getTotalQuantity());
        voucher.setUsedQuantity(0);
        voucher.setLimitPerUser(request.getLimitPerUser());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setActive(request.getActive() != null ? request.getActive() : true);
        voucher.setApplicableCategories(request.getApplicableCategories());
        voucher.setApplicableProducts(request.getApplicableProducts());
        voucher.setApplicableUserGroups(request.getApplicableUserGroups());

        Voucher savedVoucher = voucherRepository.save(voucher);
        return VoucherResponse.fromEntity(savedVoucher);
    }

    @Transactional
    public VoucherResponse updateVoucher(Long id, VoucherRequest request) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));

        // Check if code is being changed and if new code already exists
        if (!voucher.getCode().equals(request.getCode())) {
            if (voucherRepository.findByCode(request.getCode()).isPresent()) {
                throw new RuntimeException("Mã voucher đã tồn tại");
            }
            voucher.setCode(request.getCode().toUpperCase());
        }

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("Ngày kết thúc phải sau ngày bắt đầu");
        }

        voucher.setDescription(request.getDescription());
        voucher.setDiscountType(request.getDiscountType());
        voucher.setDiscountValue(request.getDiscountValue());
        voucher.setMaxDiscount(request.getMaxDiscount());
        voucher.setMinOrderValue(request.getMinOrderValue());
        voucher.setTotalQuantity(request.getTotalQuantity());
        voucher.setLimitPerUser(request.getLimitPerUser());
        voucher.setStartDate(request.getStartDate());
        voucher.setEndDate(request.getEndDate());
        voucher.setActive(request.getActive());
        voucher.setApplicableCategories(request.getApplicableCategories());
        voucher.setApplicableProducts(request.getApplicableProducts());
        voucher.setApplicableUserGroups(request.getApplicableUserGroups());

        Voucher updatedVoucher = voucherRepository.save(voucher);
        return VoucherResponse.fromEntity(updatedVoucher);
    }

    @Transactional
    public void deleteVoucher(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));
        
        // Check if voucher has been used
        if (voucher.getUsedQuantity() > 0) {
            throw new RuntimeException("Không thể xóa voucher đã được sử dụng");
        }
        
        voucherRepository.delete(voucher);
    }

    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(
            String code,
            String discountType,
            Boolean active,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        DiscountType type = null;
        if (discountType != null && !discountType.isEmpty()) {
            try {
                type = DiscountType.valueOf(discountType);
            } catch (IllegalArgumentException e) {
                // Ignore invalid discount type
            }
        }
        
        Page<Voucher> voucherPage = voucherRepository.findAllWithFilters(
            code,
            type,
            active,
            status,
            LocalDateTime.now(),
            pageable
        );
        
        return voucherPage.map(VoucherResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));
        return VoucherResponse.fromEntity(voucher);
    }

    @Transactional
    public VoucherResponse toggleVoucherStatus(Long id) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));
        
        voucher.setActive(!voucher.getActive());
        Voucher updatedVoucher = voucherRepository.save(voucher);
        return VoucherResponse.fromEntity(updatedVoucher);
    }

    @Transactional(readOnly = true)
    public VoucherStatsResponse getStatistics() {
        LocalDateTime now = LocalDateTime.now();
        
        long total = voucherRepository.count();
        long active = voucherRepository.countActiveVouchers(now);
        long upcoming = voucherRepository.countUpcomingVouchers(now);
        long expired = voucherRepository.countExpiredVouchers(now);
        long totalUsage = voucherRepository.sumTotalUsage();
        
        return new VoucherStatsResponse(total, active, expired, upcoming, totalUsage);
    }

    public String generateRandomCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
