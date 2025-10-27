package t4m.toy_store.order.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.exception.UserNotFoundException;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.cart.entity.Cart;
import t4m.toy_store.cart.entity.CartItem;
import t4m.toy_store.cart.repository.CartRepository;
import t4m.toy_store.order.dto.CheckoutRequest;
import t4m.toy_store.order.dto.OrderItemResponse;
import t4m.toy_store.order.dto.OrderResponse;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.order.entity.OrderItem;
import t4m.toy_store.order.entity.OrderStatus;
import t4m.toy_store.order.repository.OrderRepository;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.ProductRepository;
import t4m.toy_store.voucher.entity.Voucher;
import t4m.toy_store.voucher.dto.VoucherValidationResponse;
import t4m.toy_store.voucher.service.VoucherService;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final VoucherService voucherService;

    @Transactional
    public OrderResponse createOrder(String userEmail, CheckoutRequest request) {
        logger.info("Creating order for user: {}", userEmail);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Calculate cart subtotal
        BigDecimal subtotal = cart.getTotalPrice();
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        
        // Validate and apply voucher if provided
        if (request.getVoucherCode() != null && !request.getVoucherCode().trim().isEmpty()) {
            try {
                VoucherValidationResponse validationResponse = voucherService.validateVoucher(
                    request.getVoucherCode(), 
                    subtotal, 
                    user
                );
                
                if (!validationResponse.isValid()) {
                    throw new RuntimeException(validationResponse.getMessage());
                }
                
                voucherDiscount = validationResponse.getDiscountAmount();
                appliedVoucher = voucherService.getVoucherByCode(request.getVoucherCode());
                
                if (appliedVoucher == null) {
                    throw new RuntimeException("Không thể tìm thấy mã giảm giá");
                }
                
                logger.info("Voucher applied: {} with discount: {}", appliedVoucher.getCode(), voucherDiscount);
            } catch (Exception e) {
                logger.warn("Failed to apply voucher: {}", e.getMessage());
                throw new RuntimeException("Mã giảm giá không hợp lệ: " + e.getMessage());
            }
        }

        // Calculate final total
        BigDecimal finalTotal = subtotal.subtract(voucherDiscount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // Determine order status and payment status based on payment method
        OrderStatus initialStatus;
        String paymentStatus;
        
        if ("E_WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
            // For E_WALLET, wait for VNPay confirmation
            initialStatus = OrderStatus.PENDING_PAYMENT;
            paymentStatus = "PENDING";
        } else {
            // For COD and other methods
            initialStatus = OrderStatus.PENDING;
            paymentStatus = "COD".equalsIgnoreCase(request.getPaymentMethod()) ? "PENDING" : "PENDING";
        }
        
        // Create order
        Order order = Order.builder()
                .user(user)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .shippingAddress(request.getShippingAddress())
                .paymentMethod(request.getPaymentMethod())
                .totalAmount(finalTotal)
                .status(initialStatus)
                .notes(request.getNotes())
                .voucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null)
                .voucherDiscount(voucherDiscount)
                .voucherType(appliedVoucher != null ? appliedVoucher.getDiscountType().name() : null)
                .paymentStatus(paymentStatus)
                .build();

        // Add order items from cart
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            
            // Check stock
            if (product.getStock() == null || product.getStock() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(product.getImageUrl())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .build();
            
            order.addItem(orderItem);

            // Update product stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        // Save order
        Order savedOrder = orderRepository.save(order);
        
        // Record voucher usage if applied
        if (appliedVoucher != null) {
            voucherService.recordVoucherUsage(appliedVoucher, user);
            logger.info("Voucher usage recorded for: {}", appliedVoucher.getCode());
        }

        // Clear cart
        cart.getCartItems().clear();
        cartRepository.save(cart);

        logger.info("Order created successfully: {}", savedOrder.getOrderNumber());
        
        return convertToOrderResponse(savedOrder);
    }

    public List<OrderResponse> getUserOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::convertToOrderResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        return convertToOrderResponse(order);
    }

    private OrderResponse convertToOrderResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productImageUrl(item.getProductImageUrl())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .paymentMethod(order.getPaymentMethod())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .notes(order.getNotes())
                .items(items)
                .createdAt(order.getCreatedAt())
                .voucherCode(order.getVoucherCode())
                .voucherDiscount(order.getVoucherDiscount())
                .voucherType(order.getVoucherType())
                .build();
    }

    // Admin methods
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Page<Order> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    public long getTotalOrders() {
        return orderRepository.count();
    }

    public long getOrderCountByStatus(OrderStatus status) {
        return orderRepository.countByStatus(status);
    }

    // Revenue statistics
    public BigDecimal getTotalRevenue() {
        List<Order> orders = orderRepository.findAll();
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getMonthlyRevenue() {
        List<Order> orders = orderRepository.findAll();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(startOfMonth))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTodayRevenue() {
        List<Order> orders = orderRepository.findAll();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        
        return orders.stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(startOfDay))
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getAverageOrderValue() {
        List<Order> completedOrders = orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                .collect(Collectors.toList());
        
        if (completedOrders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal total = completedOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return total.divide(new BigDecimal(completedOrders.size()), 0, java.math.RoundingMode.HALF_UP);
    }

    public long getTodayOrderCount() {
        List<Order> orders = orderRepository.findAll();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0);
        
        return orders.stream()
                .filter(order -> order.getCreatedAt() != null && order.getCreatedAt().isAfter(startOfDay))
                .count();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        // Find order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        
        // Find user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Verify order belongs to user
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
        }
        
        // Check if order can be cancelled (only PENDING and COD)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng đang chờ xử lý");
        }
        
        // Only allow cancellation for COD orders
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            throw new RuntimeException("Chỉ có thể hủy đơn hàng thanh toán COD. Vui lòng liên hệ hỗ trợ để hủy đơn hàng thanh toán online.");
        }
        
        // Update order status to CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        
        logger.info("Order {} cancelled by user {}", orderId, userEmail);
        
        return convertToOrderResponse(cancelledOrder);
    }

    // VNPay payment methods
    
    /**
     * Check if order exists by order number
     */
    public boolean orderExists(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).isPresent();
    }

    /**
     * Verify if order amount matches VNPay amount
     */
    public boolean verifyOrderAmount(String orderNumber, long vnpayAmount) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            return false;
        }
        long orderAmount = order.getTotalAmount().longValue();
        return orderAmount == vnpayAmount;
    }

    /**
     * Check if payment already confirmed
     */
    public boolean isPaymentConfirmed(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            return false;
        }
        return "PAID".equals(order.getPaymentStatus());
    }

    /**
     * Update VNPay payment status
     */
    @Transactional
    public void updateVNPayPaymentStatus(String orderNumber, boolean isSuccess, 
                                          String transactionNo, String bankCode, String responseCode) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderNumber));

        if (isSuccess) {
            // Payment successful - confirm order
            order.setPaymentStatus("PAID");
            order.setStatus(OrderStatus.PENDING); // Change from PENDING_PAYMENT to PENDING (ready for processing)
            logger.info("Payment successful for order {}", orderNumber);
        } else {
            // Payment failed - cancel order and restore stock
            order.setPaymentStatus("FAILED");
            order.setStatus(OrderStatus.CANCELLED);
            
            // Restore product stock
            for (OrderItem item : order.getOrderItems()) {
                Product product = item.getProduct();
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
                logger.info("Restored stock for product {}: +{}", product.getName(), item.getQuantity());
            }
            
            // Restore voucher usage if applied
            if (order.getVoucherCode() != null) {
                try {
                    Voucher voucher = voucherService.getVoucherByCode(order.getVoucherCode());
                    if (voucher != null) {
                        voucherService.restoreVoucherUsage(voucher, order.getUser());
                        logger.info("Restored voucher usage for: {}", order.getVoucherCode());
                    }
                } catch (Exception e) {
                    logger.warn("Failed to restore voucher usage: {}", e.getMessage());
                }
            }
            
            logger.info("Payment failed for order {} - order cancelled and stock restored", orderNumber);
        }

        order.setVnpayTransactionNo(transactionNo);
        order.setVnpayBankCode(bankCode);
        order.setVnpayResponseCode(responseCode);
        
        orderRepository.save(order);
        
        logger.info("Updated VNPay payment status for order {}: status={}, transactionNo={}", 
                    orderNumber, order.getPaymentStatus(), transactionNo);
    }
}
