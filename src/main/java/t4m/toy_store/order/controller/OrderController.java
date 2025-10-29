package t4m.toy_store.order.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.order.dto.CheckoutRequest;
import t4m.toy_store.order.dto.OrderResponse;
import t4m.toy_store.order.service.OrderService;
import t4m.toy_store.payment.service.VNPayService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final VNPayService vnPayService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            HttpServletRequest httpRequest) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            // Create order first
            OrderResponse order = orderService.createOrder(email, request);
            
            // If payment method is E_WALLET, create VNPay payment URL
            if ("E_WALLET".equalsIgnoreCase(request.getPaymentMethod())) {
                String paymentUrl = vnPayService.createPaymentUrl(
                    order.getOrderNumber(),
                    order.getTotalAmount(),
                    "Thanh toan don hang " + order.getOrderNumber(),
                    httpRequest
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("order", order);
                response.put("paymentUrl", paymentUrl);
                response.put("redirectToPayment", true);
                
                return ResponseEntity.ok(response);
            }
            
            // For COD, just return order
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            List<OrderResponse> orders = orderService.getUserOrders(email);
            return ResponseEntity.ok(orders);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<?> getOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            OrderResponse order = orderService.getOrderByNumber(orderNumber);
            
            // Check if user owns this order or is admin
            boolean isOwner = order.getCustomerEmail().equalsIgnoreCase(email);
            boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isOwner && !isAdmin) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Bạn không có quyền xem đơn hàng này");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }
            
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            OrderResponse cancelledOrder = orderService.cancelOrder(orderId, email);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Cancel order by order number (for payment-pending page)
     */
    @PostMapping("/{orderNumber}/cancel")
    public ResponseEntity<?> cancelOrderByNumber(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            OrderResponse cancelledOrder = orderService.cancelOrderByNumber(orderNumber, email);
            return ResponseEntity.ok(cancelledOrder);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Public endpoint to get order details after VNPay payment
     * This endpoint doesn't require authentication because user may lose JWT token
     * after redirecting from VNPay
     */
    @GetMapping("/public/{orderNumber}")
    public ResponseEntity<?> getOrderPublic(@PathVariable String orderNumber) {
        try {
            OrderResponse order = orderService.getOrderByNumber(orderNumber);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
