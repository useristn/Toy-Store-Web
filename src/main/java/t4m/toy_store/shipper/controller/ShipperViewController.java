package t4m.toy_store.shipper.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/shipper")
public class ShipperViewController {
    
    // Note: Authorization is checked on frontend via shipper-dashboard.js
    // Backend API calls will be protected by @PreAuthorize in API controllers
    
    @GetMapping
    public String shipperDashboard() {
        return "shipper/shipper-dashboard";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "shipper/shipper-dashboard";
    }
}
