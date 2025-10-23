package t4m.toy_store.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.admin.dto.OrderStatusUpdateRequest;
import t4m.toy_store.order.dto.OrderResponse;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.order.entity.OrderStatus;
import t4m.toy_store.order.service.OrderService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminOrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OrderStatus status) {
        
        Page<Order> orders;
        if (status != null) {
            orders = orderService.getOrdersByStatus(status, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        } else {
            orders = orderService.getAllOrders(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }
        
        Page<OrderResponse> response = orders.map(OrderResponse::fromEntity);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(OrderResponse.fromEntity(order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @PathVariable Long id, 
            @RequestBody OrderStatusUpdateRequest request) {
        try {
            Order order = orderService.updateOrderStatus(id, request.getStatus());
            if (order == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Order not found");
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(OrderResponse.fromEntity(order));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOrderStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total", orderService.getTotalOrders());
        stats.put("pending", orderService.getOrderCountByStatus(OrderStatus.PENDING));
        stats.put("processing", orderService.getOrderCountByStatus(OrderStatus.PROCESSING));
        stats.put("shipped", orderService.getOrderCountByStatus(OrderStatus.SHIPPING)); // SHIPPING enum
        stats.put("delivered", orderService.getOrderCountByStatus(OrderStatus.DELIVERED));
        stats.put("failed", orderService.getOrderCountByStatus(OrderStatus.FAILED));
        stats.put("cancelled", orderService.getOrderCountByStatus(OrderStatus.CANCELLED));
        
        return ResponseEntity.ok(stats);
    }
}
