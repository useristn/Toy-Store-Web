package t4m.toy_store.order.entity;

public enum OrderStatus {
    PENDING,        // Chờ xử lý
    CONFIRMED,      // Đã xác nhận
    PROCESSING,     // Đang xử lý
    SHIPPING,       // Đang giao hàng
    DELIVERED,      // Đã giao hàng
    FAILED,         // Giao hàng thất bại
    CANCELLED,      // Đã hủy
    REFUNDED        // Đã hoàn tiền
}
