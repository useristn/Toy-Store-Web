//package t4m.toy_store.admin.service;
//
//import t4m.toy_store.user.repository.UserRepository;
//import t4m.toy_store.product.repository.ProductRepository;
//import t4m.toy_store.order.repository.OrderRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class AdminDashboardService {
//
//    private final UserRepository userRepo;
//    private final ProductRepository productRepo;
//    private final OrderRepository orderRepo;
//
//    public Map<String, Long> getStats() {
//        Map<String, Long> stats = new HashMap<>();
//        stats.put("users", userRepo.count());
//        stats.put("products", productRepo.count());
//        stats.put("orders", orderRepo.count());
//        return stats;
//    }
//}
