//package t4m.toy_store.admin.controller;
//
////# quản trị dashboard
//import t4m.toy_store.admin.service.AdminDashboardService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/admin/dashboard")
//@RequiredArgsConstructor
//public class AdminDashboardController {
//
//    private final AdminDashboardService dashboardService;
//
//    @GetMapping("/stats")
//    public Map<String, Long> getStats() {
//        return dashboardService.getStats();
//    }
//}
