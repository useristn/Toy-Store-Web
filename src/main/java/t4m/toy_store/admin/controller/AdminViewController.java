package t4m.toy_store.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminViewController {

    // Note: Authorization is checked on frontend via auth.js
    // Backend API calls will be protected by @PreAuthorize in API controllers

    @GetMapping
    public String adminDashboard() {
        return "admin/admin-dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/admin-dashboard";
    }

    @GetMapping("/products")
    public String products() {
        return "admin/admin-products";
    }

    @GetMapping("/orders")
    public String orders() {
        return "admin/admin-orders";
    }

    @GetMapping("/support")
    public String support() {
        return "admin/admin-support";
    }

    @GetMapping("/reviews")
    public String reviews() {
        return "admin/admin-reviews";
    }
}
