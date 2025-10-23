package t4m.toy_store.shipper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.order.dto.OrderResponse;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.shipper.service.ShipperService;
import t4m.toy_store.shipper.service.ShipperStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shipper")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_SHIPPER')")
public class ShipperController {
    
    private final ShipperService shipperService;

    /**
     * Lấy danh sách đơn hàng liên quan đến shipper
     * Bao gồm: đơn có thể nhận (PROCESSING) và đơn đang giao (SHIPPING)
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getShipperOrders(
            @RequestHeader("X-User-Email") String shipperEmail) {
        List<Order> orders = shipperService.getShipperOrders(shipperEmail);
        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách đơn hàng có thể nhận (PROCESSING)
     */
    @GetMapping("/orders/available")
    public ResponseEntity<List<OrderResponse>> getAvailableOrders() {
        List<Order> orders = shipperService.getAvailableOrders();
        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách đơn hàng đang giao của shipper
     */
    @GetMapping("/orders/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders(
            @RequestHeader("X-User-Email") String shipperEmail) {
        List<Order> orders = shipperService.getShipperActiveOrders(shipperEmail);
        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy lịch sử giao hàng của shipper
     */
    @GetMapping("/orders/history")
    public ResponseEntity<List<OrderResponse>> getDeliveryHistory(
            @RequestHeader("X-User-Email") String shipperEmail) {
        List<Order> orders = shipperService.getShipperHistory(shipperEmail);
        List<OrderResponse> response = orders.stream()
                .map(OrderResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Shipper nhận đơn hàng
     * Chuyển trạng thái từ PROCESSING -> SHIPPING và gán shipper_id
     */
    @PutMapping("/orders/{id}/accept")
    public ResponseEntity<?> acceptOrder(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String shipperEmail) {
        try {
            Order order = shipperService.acceptOrder(id, shipperEmail);
            return ResponseEntity.ok(OrderResponse.fromEntity(order));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Shipper hoàn thành giao hàng
     * Chuyển trạng thái từ SHIPPING -> DELIVERED
     */
    @PutMapping("/orders/{id}/complete")
    public ResponseEntity<?> completeDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String shipperEmail) {
        try {
            Order order = shipperService.completeDelivery(id, shipperEmail);
            return ResponseEntity.ok(OrderResponse.fromEntity(order));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Shipper báo cáo giao hàng thất bại
     * Chuyển trạng thái từ SHIPPING -> FAILED
     */
    @PutMapping("/orders/{id}/fail")
    public ResponseEntity<?> failDelivery(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String shipperEmail,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.getOrDefault("reason", "Không thể liên hệ khách hàng") : "Không thể liên hệ khách hàng";
            Order order = shipperService.failDelivery(id, shipperEmail, reason);
            return ResponseEntity.ok(OrderResponse.fromEntity(order));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Lấy chi tiết một đơn hàng
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String shipperEmail) {
        try {
            Order order = shipperService.getOrderById(id, shipperEmail);
            return ResponseEntity.ok(OrderResponse.fromEntity(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lấy thống kê cho shipper dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<ShipperStats> getStats(
            @RequestHeader("X-User-Email") String shipperEmail) {
        ShipperStats stats = shipperService.getShipperStats(shipperEmail);
        return ResponseEntity.ok(stats);
    }
}
