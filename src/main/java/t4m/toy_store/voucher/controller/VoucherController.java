package t4m.toy_store.voucher.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.voucher.dto.VoucherValidationResponse;
import t4m.toy_store.voucher.service.VoucherService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {
    
    private final VoucherService voucherService;

    @PostMapping("/validate")
    public ResponseEntity<VoucherValidationResponse> validateVoucher(
            @RequestParam String code,
            @RequestParam BigDecimal orderTotal,
            @AuthenticationPrincipal User user) {
        VoucherValidationResponse response = voucherService.validateVoucher(code, orderTotal, user);
        return ResponseEntity.ok(response);
    }
}
