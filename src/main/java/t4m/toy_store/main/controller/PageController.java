package t4m.toy_store.main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "index";
    }

    @GetMapping({"/login"})
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/verify-otp")
    public String verifyOtp() {
        return "verify-otp";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPassword() {
        return "reset-password";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/products")
    public String products() {
        return "products";
    }

    @GetMapping("/product/{id}")
    public String productDetail() {
        return "product-detail";
    }

    @GetMapping("/cart")
    public String cart() {
        return "cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "checkout";
    }

    @GetMapping("/order-confirmation/{orderNumber}")
    public String orderConfirmation() {
        return "order-confirmation";
    }

    @GetMapping("/payment-pending/{orderNumber}")
    public String paymentPending() {
        return "payment-pending";
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }
    
    @GetMapping("/test-search")
    public String testSearch() {
        return "test-search";
    }
    
    @GetMapping("/favorites")
    public String favorites() {
        return "favorites";
    }
    
    // Policy Pages
    @GetMapping("/terms")
    public String terms() {
        return "policies/terms";
    }
    
    @GetMapping("/privacy")
    public String privacy() {
        return "policies/privacy";
    }
    
    @GetMapping("/return-policy")
    public String returnPolicy() {
        return "policies/return-policy";
    }
    
    @GetMapping("/shopping-guide")
    public String shoppingGuide() {
        return "policies/shopping-guide";
    }
    
    @GetMapping("/payment-security")
    public String paymentSecurity() {
        return "policies/payment-security";
    }
}
