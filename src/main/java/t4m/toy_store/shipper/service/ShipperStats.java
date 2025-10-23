package t4m.toy_store.shipper.service;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipperStats {
    private long availableOrders;      // Số đơn hàng có thể nhận (PROCESSING)
    private long activeDeliveries;     // Số đơn đang giao (SHIPPING)
    private long completedDeliveries;  // Số đơn đã giao thành công (DELIVERED)
    private long failedDeliveries;     // Số đơn giao thất bại (FAILED)
    private long totalDeliveries;      // Tổng số đơn đã xử lý (DELIVERED + FAILED)
}
