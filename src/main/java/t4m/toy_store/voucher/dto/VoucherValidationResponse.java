package t4m.toy_store.voucher.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoucherValidationResponse {
    private boolean valid;
    private String message;
    private BigDecimal discountAmount;
    private String voucherCode;
}
