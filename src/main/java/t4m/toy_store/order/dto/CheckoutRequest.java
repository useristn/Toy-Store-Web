package t4m.toy_store.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    
    @NotBlank(message = "Họ tên không được để trống")
    private String customerName;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String customerEmail;
    
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^[0-9]{10,11}$", message = "Số điện thoại không hợp lệ")
    private String customerPhone;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;
    
    @NotBlank(message = "Phương thức thanh toán không được để trống")
    private String paymentMethod;
    
    private String notes;
}
