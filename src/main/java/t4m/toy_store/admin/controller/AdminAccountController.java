package t4m.toy_store.admin.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.admin.dto.AccountCreateRequest;
import t4m.toy_store.admin.dto.AccountDTO;
import t4m.toy_store.admin.dto.AccountUpdateRequest;
import t4m.toy_store.admin.dto.BulkActionRequest;
import t4m.toy_store.admin.service.AdminAccountService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAccountController {

    @Autowired
    private AdminAccountService accountService;

    /**
     * Get all accounts with pagination, search, and filtering
     */
    @GetMapping
    public ResponseEntity<?> getAllAccounts(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String roleFilter,
            @RequestParam(defaultValue = "") String statusFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        try {
            Page<AccountDTO> accounts = accountService.getAccounts(
                search, roleFilter, statusFilter, page, size, sortBy, sortDir
            );

            Map<String, Object> response = new HashMap<>();
            response.put("accounts", accounts.getContent());
            response.put("currentPage", accounts.getNumber());
            response.put("totalItems", accounts.getTotalElements());
            response.put("totalPages", accounts.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get account by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAccountById(@PathVariable Long id) {
        try {
            AccountDTO account = accountService.getAccountById(id);
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create new account
     */
    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        try {
            AccountDTO account = accountService.createAccount(request);
            return ResponseEntity.ok(Map.of(
                "message", "Tạo tài khoản thành công",
                "account", account
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update account
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountUpdateRequest request
    ) {
        try {
            AccountDTO account = accountService.updateAccount(id, request);
            return ResponseEntity.ok(Map.of(
                "message", "Cập nhật tài khoản thành công",
                "account", account
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete account
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable Long id) {
        try {
            accountService.deleteAccount(id);
            return ResponseEntity.ok(Map.of("message", "Xóa tài khoản thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Ban account
     */
    @PostMapping("/{id}/ban")
    public ResponseEntity<?> banAccount(@PathVariable Long id) {
        try {
            accountService.banAccount(id);
            return ResponseEntity.ok(Map.of("message", "Khóa tài khoản thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Unban account
     */
    @PostMapping("/{id}/unban")
    public ResponseEntity<?> unbanAccount(@PathVariable Long id) {
        try {
            accountService.unbanAccount(id);
            return ResponseEntity.ok(Map.of("message", "Mở khóa tài khoản thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reset password
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id) {
        try {
            accountService.resetPassword(id);
            return ResponseEntity.ok(Map.of(
                "message", "Email đặt lại mật khẩu đã được gửi đến người dùng"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Bulk actions
     */
    @PostMapping("/bulk-action")
    public ResponseEntity<?> bulkAction(@RequestBody BulkActionRequest request) {
        try {
            accountService.bulkAction(request);
            String actionMessage = switch (request.getAction().toLowerCase()) {
                case "ban" -> "Khóa các tài khoản thành công";
                case "unban" -> "Mở khóa các tài khoản thành công";
                case "delete" -> "Xóa các tài khoản thành công";
                default -> "Thực hiện hành động thành công";
            };
            return ResponseEntity.ok(Map.of("message", actionMessage));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Search accounts
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchAccounts(@RequestParam String keyword) {
        try {
            List<AccountDTO> accounts = accountService.searchAccounts(keyword);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
