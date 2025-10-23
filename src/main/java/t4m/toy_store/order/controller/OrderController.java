package t4m.toy_store.order.controller;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            // Get email from UserDetails or fallback to header
            String email = userDetails != null ? userDetails.getUsername() : userEmail;
            
            if (email == null || email.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            
            OrderResponse order = orderService.createOrder(email, request);
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
}
