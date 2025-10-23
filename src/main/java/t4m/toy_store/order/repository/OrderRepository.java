package t4m.toy_store.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.order.entity.OrderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Order> findByOrderNumber(String orderNumber);
    
    // Admin methods
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    long countByStatus(OrderStatus status);
    
    // Shipper methods
    List<Order> findByShipperIdAndStatus(Long shipperId, OrderStatus status);
    List<Order> findByShipperId(Long shipperId);
    long countByShipperIdAndStatus(Long shipperId, OrderStatus status);
    long countByShipperId(Long shipperId);
}
