package t4m.toy_store.voucher.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import t4m.toy_store.voucher.entity.DiscountType;
import t4m.toy_store.voucher.entity.Voucher;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    
    Optional<Voucher> findByCode(String code);
    
    Optional<Voucher> findByCodeAndActiveTrue(String code);
    
    @Query("SELECT v FROM Voucher v WHERE " +
           "(:code IS NULL OR LOWER(v.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
           "(:discountType IS NULL OR v.discountType = :discountType) AND " +
           "(:active IS NULL OR v.active = :active) AND " +
           "(:status IS NULL OR " +
           "  (:status = 'ACTIVE' AND v.active = true AND :now BETWEEN v.startDate AND v.endDate AND v.usedQuantity < v.totalQuantity) OR " +
           "  (:status = 'UPCOMING' AND v.active = true AND :now < v.startDate) OR " +
           "  (:status = 'EXPIRED' AND (v.active = false OR :now > v.endDate OR v.usedQuantity >= v.totalQuantity)))")
    Page<Voucher> findAllWithFilters(
        @Param("code") String code,
        @Param("discountType") DiscountType discountType,
        @Param("active") Boolean active,
        @Param("status") String status,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.active = true AND :now BETWEEN v.startDate AND v.endDate AND v.usedQuantity < v.totalQuantity")
    long countActiveVouchers(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.active = true AND :now < v.startDate")
    long countUpcomingVouchers(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.active = false OR :now > v.endDate OR v.usedQuantity >= v.totalQuantity")
    long countExpiredVouchers(@Param("now") LocalDateTime now);
    
    @Query("SELECT COALESCE(SUM(v.usedQuantity), 0) FROM Voucher v")
    long sumTotalUsage();
}
