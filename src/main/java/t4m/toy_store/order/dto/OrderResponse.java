package t4m.toy_store.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import t4m.toy_store.order.entity.Order;
import t4m.toy_store.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String shippingAddress;
    private String paymentMethod;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String notes;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private Long shipperId;
    private String shipperEmail;
    private String voucherCode;
    private BigDecimal voucherDiscount;
    private String voucherType;
    
    // VNPay payment information
    private String paymentStatus;
    private String vnpayTransactionNo;
    private String vnpayBankCode;
    private String vnpayResponseCode;

    public static OrderResponse fromEntity(Order order) {
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
                .shipperId(order.getShipper() != null ? order.getShipper().getId() : null)
                .shipperEmail(order.getShipper() != null ? order.getShipper().getEmail() : null)
                .voucherCode(order.getVoucherCode())
                .voucherDiscount(order.getVoucherDiscount())
                .voucherType(order.getVoucherType())
                .paymentStatus(order.getPaymentStatus())
                .vnpayTransactionNo(order.getVnpayTransactionNo())
                .vnpayBankCode(order.getVnpayBankCode())
                .vnpayResponseCode(order.getVnpayResponseCode())
                .build();
    }
}
