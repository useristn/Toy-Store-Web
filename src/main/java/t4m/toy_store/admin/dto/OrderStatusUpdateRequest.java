package t4m.toy_store.admin.dto;

import lombok.Data;
import t4m.toy_store.order.entity.OrderStatus;

@Data
public class OrderStatusUpdateRequest {
    private OrderStatus status;
}
