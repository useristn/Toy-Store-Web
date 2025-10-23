package t4m.toy_store.shipper.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.exception.UserNotFoundException;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.order.entity.OrderStatus;
import t4m.toy_store.order.repository.OrderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShipperService {
    
    private static final Logger logger = LoggerFactory.getLogger(ShipperService.class);
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Lấy danh sách đơn hàng có trạng thái PROCESSING (chờ shipper nhận)
     * Shipper có thể xem các đơn này để chọn nhận
     */
    public List<Order> getAvailableOrders() {
        logger.info("Getting available orders for shippers (PROCESSING status)");
        return orderRepository.findByStatus(OrderStatus.PROCESSING, null).getContent();
    }

    /**
     * Lấy danh sách đơn hàng đang giao của shipper cụ thể
     * Chỉ lấy các đơn có trạng thái SHIPPING và được gán cho shipper này
     */
    public List<Order> getShipperActiveOrders(String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        logger.info("Getting active orders for shipper: {}", shipperEmail);
        return orderRepository.findByShipperIdAndStatus(shipper.getId(), OrderStatus.SHIPPING);
    }

    /**
     * Lấy danh sách tất cả đơn hàng liên quan đến shipper
     * Bao gồm cả đơn đang giao (SHIPPING) và có thể nhận (PROCESSING)
     */
    public List<Order> getShipperOrders(String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        logger.info("Getting all orders for shipper: {}", shipperEmail);
        // Lấy các đơn đang giao của shipper này
        List<Order> shippingOrders = orderRepository.findByShipperIdAndStatus(shipper.getId(), OrderStatus.SHIPPING);
        // Thêm các đơn đang chờ nhận (PROCESSING)
        List<Order> processingOrders = getAvailableOrders();
        
        shippingOrders.addAll(processingOrders);
        return shippingOrders;
    }

    /**
     * Lấy lịch sử đơn hàng đã giao của shipper
     * Bao gồm các đơn có trạng thái DELIVERED, FAILED và CANCELLED
     */
    public List<Order> getShipperHistory(String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        logger.info("Getting delivery history for shipper: {}", shipperEmail);
        
        // Lấy các đơn đã hoàn tất (thành công hoặc thất bại)
        List<Order> history = new java.util.ArrayList<>();
        history.addAll(orderRepository.findByShipperIdAndStatus(shipper.getId(), OrderStatus.DELIVERED));
        history.addAll(orderRepository.findByShipperIdAndStatus(shipper.getId(), OrderStatus.FAILED));
        history.addAll(orderRepository.findByShipperIdAndStatus(shipper.getId(), OrderStatus.CANCELLED));
        
        // Sắp xếp theo thời gian tạo giảm dần
        history.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
        
        return history;
    }

    /**
     * Shipper nhận đơn hàng
     * Chuyển trạng thái từ PROCESSING -> SHIPPING và gán shipper_id
     */
    @Transactional
    public Order acceptOrder(Long orderId, String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() != OrderStatus.PROCESSING) {
            throw new RuntimeException("Chỉ có thể nhận đơn hàng đang ở trạng thái 'Đang xử lý'");
        }
        
        // Kiểm tra đơn hàng đã được shipper khác nhận chưa
        if (order.getShipper() != null) {
            throw new RuntimeException("Đơn hàng này đã được shipper khác nhận");
        }
        
        // Cập nhật trạng thái và gán shipper
        order.setStatus(OrderStatus.SHIPPING);
        order.setShipper(shipper);
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} accepted by shipper {}", orderId, shipperEmail);
        
        return savedOrder;
    }

    /**
     * Shipper hoàn thành giao hàng
     * Chuyển trạng thái từ SHIPPING -> DELIVERED
     */
    @Transactional
    public Order completeDelivery(Long orderId, String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new RuntimeException("Chỉ có thể hoàn thành đơn hàng đang ở trạng thái 'Đang giao'");
        }
        
        // Kiểm tra đơn hàng có thuộc về shipper này không
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            throw new RuntimeException("Bạn không có quyền hoàn thành đơn hàng này");
        }
        
        // Cập nhật trạng thái
        order.setStatus(OrderStatus.DELIVERED);
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} completed by shipper {}", orderId, shipperEmail);
        
        return savedOrder;
    }

    /**
     * Lấy chi tiết một đơn hàng
     */
    public Order getOrderById(Long orderId, String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra quyền xem đơn hàng:
        // - Đơn đang PROCESSING (có thể nhận) thì tất cả shipper đều xem được
        // - Đơn đang SHIPPING, DELIVERED, FAILED thì chỉ shipper được gán mới xem được
        if (order.getStatus() != OrderStatus.PROCESSING) {
            if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
                throw new RuntimeException("Bạn không có quyền xem đơn hàng này");
            }
        }
        
        logger.info("Shipper {} viewed order {}", shipperEmail, orderId);
        return order;
    }

    /**
     * Lấy thống kê cho shipper dashboard
     */
    public ShipperStats getShipperStats(String shipperEmail) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        long availableOrders = orderRepository.countByStatus(OrderStatus.PROCESSING);
        long activeDeliveries = orderRepository.countByShipperIdAndStatus(shipper.getId(), OrderStatus.SHIPPING);
        long completedDeliveries = orderRepository.countByShipperIdAndStatus(shipper.getId(), OrderStatus.DELIVERED);
        long failedDeliveries = orderRepository.countByShipperIdAndStatus(shipper.getId(), OrderStatus.FAILED);
        // Tổng đã giao = Đã giao thành công + Giao thất bại
        long totalDeliveries = completedDeliveries + failedDeliveries;
        
        logger.info("Shipper {} stats - Available: {}, Active: {}, Completed: {}, Failed: {}, Total: {}", 
                    shipperEmail, availableOrders, activeDeliveries, completedDeliveries, failedDeliveries, totalDeliveries);
        
        return ShipperStats.builder()
                .availableOrders(availableOrders)
                .activeDeliveries(activeDeliveries)
                .completedDeliveries(completedDeliveries)
                .failedDeliveries(failedDeliveries)
                .totalDeliveries(totalDeliveries)
                .build();
    }

    /**
     * Shipper báo cáo giao hàng thất bại
     * Chuyển trạng thái từ SHIPPING -> FAILED
     */
    @Transactional
    public Order failDelivery(Long orderId, String shipperEmail, String reason) {
        User shipper = userRepository.findByEmail(shipperEmail)
                .orElseThrow(() -> new UserNotFoundException("Shipper not found"));
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        // Kiểm tra trạng thái đơn hàng
        if (order.getStatus() != OrderStatus.SHIPPING) {
            throw new RuntimeException("Chỉ có thể báo cáo thất bại cho đơn hàng đang ở trạng thái 'Đang giao'");
        }
        
        // Kiểm tra đơn hàng có thuộc về shipper này không
        if (order.getShipper() == null || !order.getShipper().getId().equals(shipper.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật đơn hàng này");
        }
        
        // Cập nhật trạng thái
        order.setStatus(OrderStatus.FAILED);
        // Có thể thêm trường note để lưu lý do nếu cần
        // order.setNote(reason);
        
        Order savedOrder = orderRepository.save(order);
        logger.info("Order {} marked as failed by shipper {}. Reason: {}", orderId, shipperEmail, reason);
        
        return savedOrder;
    }
}
