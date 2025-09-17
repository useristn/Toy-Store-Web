package t4m.toy_store.admin.service;

import t4m.toy_store.admin.entity.Coupon;
import t4m.toy_store.admin.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {
    private final CouponRepository couponRepository;

    public List<Coupon> findAll() { return couponRepository.findAll(); }

    public Coupon create(Coupon coupon) { return couponRepository.save(coupon); }

    public Coupon update(Long id, Coupon data) {
        return couponRepository.findById(id)
                .map(c -> {
                    c.setCode(data.getCode());
                    c.setDiscountPercent(data.getDiscountPercent());
                    c.setStartDate(data.getStartDate());
                    c.setEndDate(data.getEndDate());
                    c.setActive(data.isActive());
                    return couponRepository.save(c);
                })
                .orElseThrow(() -> new RuntimeException("Coupon not found"));
    }

    public void delete(Long id) { couponRepository.deleteById(id); }
}
