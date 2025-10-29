package t4m.toy_store.voucher.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.voucher.dto.VoucherRequest;
import t4m.toy_store.voucher.dto.VoucherResponse;
import t4m.toy_store.voucher.dto.VoucherStatsResponse;
import t4m.toy_store.voucher.service.AdminVoucherService;

@RestController
@RequestMapping("/api/admin/vouchers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {
    
    private final AdminVoucherService adminVoucherService;

    @PostMapping
    public ResponseEntity<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        try {
            VoucherResponse response = adminVoucherService.createVoucher(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoucherResponse> updateVoucher(
            @PathVariable Long id,
            @Valid @RequestBody VoucherRequest request) {
        try {
            VoucherResponse response = adminVoucherService.updateVoucher(id, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVoucher(@PathVariable Long id) {
        try {
            adminVoucherService.deleteVoucher(id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<VoucherResponse>> getAllVouchers(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Page<VoucherResponse> vouchers = adminVoucherService.getAllVouchers(
            code, discountType, active, status, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(vouchers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoucherResponse> getVoucherById(@PathVariable Long id) {
        try {
            VoucherResponse response = adminVoucherService.getVoucherById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<VoucherResponse> toggleVoucherStatus(@PathVariable Long id) {
        try {
            VoucherResponse response = adminVoucherService.toggleVoucherStatus(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<VoucherStatsResponse> getStatistics() {
        VoucherStatsResponse stats = adminVoucherService.getStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/generate-code")
    public ResponseEntity<String> generateCode(@RequestParam(defaultValue = "8") int length) {
        String code = adminVoucherService.generateRandomCode(length);
        return ResponseEntity.ok(code);
    }
}
