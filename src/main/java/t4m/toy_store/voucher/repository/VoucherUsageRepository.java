package t4m.toy_store.voucher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import t4m.toy_store.voucher.entity.Voucher;
import t4m.toy_store.voucher.entity.VoucherUsage;
import t4m.toy_store.auth.entity.User;

import java.util.List;

@Repository
public interface VoucherUsageRepository extends JpaRepository<VoucherUsage, Long> {
    
    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucher = :voucher AND vu.user = :user")
    long countByVoucherAndUser(@Param("voucher") Voucher voucher, @Param("user") User user);
    
    List<VoucherUsage> findByVoucher(Voucher voucher);
    
    List<VoucherUsage> findByUser(User user);
}
