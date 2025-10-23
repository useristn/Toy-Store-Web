package t4m.toy_store.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/init")
@RequiredArgsConstructor
public class AdminInitController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * TEMPORARY ENDPOINT - Khởi tạo admin user
     * Xóa hoặc comment endpoint này sau khi đã tạo admin!
     * 
     * POST /api/admin/init/create-admin
     * Body: {
     *   "email": "admin@toystore.com",
     *   "password": "admin123",
     *   "name": "Admin User"
     * }
     */
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String email = request.get("email");
            String password = request.get("password");
            String name = request.getOrDefault("name", "Admin User");

            // Check if user already exists
            if (userRepository.findByEmail(email).isPresent()) {
                response.put("error", "User with this email already exists");
                return ResponseEntity.badRequest().body(response);
            }

            // Create or get ROLE_ADMIN
            Role adminRole = roleRepository.findByRname("ROLE_ADMIN")
                    .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setRname("ROLE_ADMIN");
                        return roleRepository.save(newRole);
                    });

            // Create admin user
            User adminUser = new User();
            adminUser.setEmail(email);
            adminUser.setPasswd(passwordEncoder.encode(password));
            adminUser.setName(name);
            adminUser.setPhone("0000000000");
            adminUser.setAddress("Admin Address");
            adminUser.setActivated(true);
            adminUser.getRoles().add(adminRole);

            User savedUser = userRepository.save(adminUser);

            response.put("success", true);
            response.put("message", "Admin user created successfully");
            response.put("userId", savedUser.getId());
            response.put("email", savedUser.getEmail());
            response.put("name", savedUser.getName());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Check if admin exists
     * GET /api/admin/init/check
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAdminExists() {
        Map<String, Object> response = new HashMap<>();
        
        Optional<Role> adminRole = roleRepository.findByRname("ROLE_ADMIN");
        
        if (adminRole.isEmpty()) {
            response.put("adminRoleExists", false);
            response.put("adminUsersCount", 0);
        } else {
            long adminCount = userRepository.findAll().stream()
                    .filter(user -> user.getRoles().stream()
                            .anyMatch(role -> "ROLE_ADMIN".equals(role.getRname())))
                    .count();
            
            response.put("adminRoleExists", true);
            response.put("adminUsersCount", adminCount);
        }
        
        return ResponseEntity.ok(response);
    }
}
