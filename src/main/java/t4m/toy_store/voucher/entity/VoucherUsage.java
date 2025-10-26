package t4m.toy_store.voucher.entity;

import jakarta.persistence.*;
import lombok.Data;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.order.entity.Order;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "voucher_usage")
public class VoucherUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt = LocalDateTime.now();
}
