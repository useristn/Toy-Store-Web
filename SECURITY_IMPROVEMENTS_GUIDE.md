# HƯỚNG DẪN CẢI THIỆN BẢO MẬT
## Implementation Guide for Security Improvements

---

## 🔴 ĐÃ FIX: Order Authorization (CRITICAL)

### Vấn đề
Trước đây bất kỳ authenticated user nào cũng có thể xem order của người khác.

### Giải pháp đã triển khai
Updated `OrderController.getOrderByNumber()`:
```java
@GetMapping("/{orderNumber}")
public ResponseEntity<?> getOrderByNumber(
        @PathVariable String orderNumber,
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
    
    String email = userDetails != null ? userDetails.getUsername() : userEmail;
    OrderResponse order = orderService.getOrderByNumber(orderNumber);
    
    // ✅ Check ownership or admin role
    boolean isOwner = order.getCustomerEmail().equalsIgnoreCase(email);
    boolean isAdmin = userDetails.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    
    if (!isOwner && !isAdmin) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "Bạn không có quyền xem đơn hàng này"));
    }
    
    return ResponseEntity.ok(order);
}
```

**Kết quả:** ✅ FIXED - Chỉ owner hoặc admin mới xem được order

---

## 🟡 CẦN IMPLEMENT: Token Refresh Mechanism

### Bước 1: Tạo RefreshToken Entity

**File mới:** `src/main/java/t4m/toy_store/auth/entity/RefreshToken.java`

```java
package t4m.toy_store.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String token;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime expiryDate;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}
```

### Bước 2: Tạo RefreshToken Repository

**File mới:** `src/main/java/t4m/toy_store/auth/repository/RefreshTokenRepository.java`

```java
package t4m.toy_store.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import t4m.toy_store.auth.entity.RefreshToken;
import t4m.toy_store.auth.entity.User;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    
    @Modifying
    int deleteByUser(User user);
}
```

### Bước 3: Tạo RefreshToken Service

**File mới:** `src/main/java/t4m/toy_store/auth/service/RefreshTokenService.java`

```java
package t4m.toy_store.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.entity.RefreshToken;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RefreshTokenRepository;
import t4m.toy_store.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    
    @Value("${jwt.refresh.expiration:604800000}") // 7 days default
    private Long refreshTokenDurationMs;
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    
    public RefreshToken createRefreshToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000))
                .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
    
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại!");
        }
        return token;
    }
    
    @Transactional
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return refreshTokenRepository.deleteByUser(user);
    }
}
```

### Bước 4: Update AuthController

**Add to:** `src/main/java/t4m/toy_store/auth/controller/AuthController.java`

```java
// Add to class fields
private final RefreshTokenService refreshTokenService;

// Update login method to return refresh token
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    try {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(request.getEmail());
        String jwt = jwtUtil.generateToken(request.getEmail());
        
        // ✅ Generate refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(request.getEmail());
        
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        AuthResponse response = new AuthResponse(
            jwt, 
            refreshToken.getToken(), // Add this
            user.getEmail(), 
            user.getRole().name()
        );
        
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse("Invalid credentials"));
    }
}

// ✅ Add new refresh endpoint
@PostMapping("/refresh")
public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
    String requestRefreshToken = request.getRefreshToken();
    
    return refreshTokenService.findByToken(requestRefreshToken)
        .map(refreshTokenService::verifyExpiration)
        .map(RefreshToken::getUser)
        .map(user -> {
            String newAccessToken = jwtUtil.generateToken(user.getEmail());
            return ResponseEntity.ok(new TokenRefreshResponse(
                newAccessToken,
                requestRefreshToken,
                user.getEmail()
            ));
        })
        .orElseThrow(() -> new RuntimeException("Refresh token không hợp lệ!"));
}

// ✅ Add logout endpoint
@PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
    try {
        String token = authHeader.substring(7);
        String email = jwtUtil.extractUsername(token);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        refreshTokenService.deleteByUserId(user.getId());
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công!"));
    } catch (Exception e) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", e.getMessage()));
    }
}
```

### Bước 5: Tạo DTOs

**File mới:** `src/main/java/t4m/toy_store/auth/dto/RefreshTokenRequest.java`

```java
package t4m.toy_store.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
```

**File mới:** `src/main/java/t4m/toy_store/auth/dto/TokenRefreshResponse.java`

```java
package t4m.toy_store.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenRefreshResponse {
    private String accessToken;
    private String refreshToken;
    private String email;
}
```

### Bước 6: Update AuthResponse

**Update:** `src/main/java/t4m/toy_store/auth/dto/AuthResponse.java`

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken; // ✅ Add this
    private String email;
    private String role;
}
```

### Bước 7: Update Frontend (auth.js)

```javascript
// Update login function
async function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    const response = await fetch(`${apiBase}/login`, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email, password})
    });

    if (response.ok) {
        const data = await response.json();
        
        // ✅ Save refresh token
        localStorage.setItem('authToken', data.token);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('authEmail', data.email);
        localStorage.setItem('userRole', data.role);
        
        window.location.href = '/';
    }
}

// ✅ Add automatic token refresh
async function refreshAccessToken() {
    const refreshToken = localStorage.getItem('refreshToken');
    
    if (!refreshToken) {
        return false;
    }
    
    try {
        const response = await fetch(`${apiBase}/refresh`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({refreshToken})
        });
        
        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('authToken', data.accessToken);
            return true;
        } else {
            // Refresh token expired, need to login again
            localStorage.clear();
            window.location.href = '/login?error=session_expired';
            return false;
        }
    } catch (error) {
        console.error('Token refresh failed:', error);
        return false;
    }
}

// ✅ Intercept 401 errors and auto-refresh
async function fetchWithAuth(url, options = {}) {
    const token = localStorage.getItem('authToken');
    const email = localStorage.getItem('authEmail');
    
    options.headers = {
        ...options.headers,
        'Authorization': `Bearer ${token}`,
        'X-User-Email': email
    };
    
    let response = await fetch(url, options);
    
    // If 401, try to refresh token
    if (response.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) {
            // Retry original request with new token
            const newToken = localStorage.getItem('authToken');
            options.headers['Authorization'] = `Bearer ${newToken}`;
            response = await fetch(url, options);
        }
    }
    
    return response;
}
```

### Bước 8: Database Migration

```sql
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    expiry_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_token (token),
    INDEX idx_user_id (user_id)
);
```

---

## 🟡 CẦN IMPLEMENT: Rate Limiting

### Bước 1: Add Dependency

**Update:** `pom.xml`

```xml
<!-- Resilience4j for rate limiting -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### Bước 2: Configure Rate Limiting

**Add to:** `application.properties`

```properties
# Rate Limiting Configuration
resilience4j.ratelimiter.instances.loginLimiter.limitForPeriod=5
resilience4j.ratelimiter.instances.loginLimiter.limitRefreshPeriod=60s
resilience4j.ratelimiter.instances.loginLimiter.timeoutDuration=0s

resilience4j.ratelimiter.instances.apiLimiter.limitForPeriod=100
resilience4j.ratelimiter.instances.apiLimiter.limitRefreshPeriod=60s
resilience4j.ratelimiter.instances.apiLimiter.timeoutDuration=0s
```

### Bước 3: Apply Rate Limiting

**Update:** `AuthController.java`

```java
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

@RateLimiter(name = "loginLimiter", fallbackMethod = "loginFallback")
@PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    // existing code
}

// Fallback method
public ResponseEntity<?> loginFallback(LoginRequest request, Exception e) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(new ErrorResponse("Quá nhiều lần thử đăng nhập. Vui lòng thử lại sau 1 phút."));
}

@RateLimiter(name = "loginLimiter", fallbackMethod = "registerFallback")
@PostMapping("/register")
public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
    // existing code
}

public ResponseEntity<?> registerFallback(RegisterRequest request, Exception e) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .body(new ErrorResponse("Quá nhiều lần đăng ký. Vui lòng thử lại sau 1 phút."));
}
```

---

## 🟡 CẦN IMPLEMENT: Password Strength Validation

### Bước 1: Tạo Custom Annotation

**File mới:** `src/main/java/t4m/toy_store/auth/validation/StrongPassword.java`

```java
package t4m.toy_store.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    
    String message() default "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    int minLength() default 8;
    boolean requireUppercase() default true;
    boolean requireLowercase() default true;
    boolean requireDigit() default true;
    boolean requireSpecial() default true;
}
```

### Bước 2: Tạo Validator

**File mới:** `src/main/java/t4m/toy_store/auth/validation/StrongPasswordValidator.java`

```java
package t4m.toy_store.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecial;
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecial = constraintAnnotation.requireSpecial();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < minLength) {
            return false;
        }
        
        if (requireUppercase && !password.matches(".*[A-Z].*")) {
            return false;
        }
        
        if (requireLowercase && !password.matches(".*[a-z].*")) {
            return false;
        }
        
        if (requireDigit && !password.matches(".*\\d.*")) {
            return false;
        }
        
        if (requireSpecial && !password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            return false;
        }
        
        return true;
    }
}
```

### Bước 3: Apply to DTOs

**Update:** `RegisterRequest.java` và `ResetPasswordRequest.java`

```java
@Data
public class RegisterRequest {
    @Email(message = "Email không hợp lệ")
    private String email;
    
    @StrongPassword  // ✅ Add this
    private String password;
    
    private String role;
}
```

---

## 🟢 CẦN IMPLEMENT: Audit Logging

### Bước 1: Tạo AuditLog Entity

**File mới:** `src/main/java/t4m/toy_store/audit/entity/AuditLog.java`

```java
package t4m.toy_store.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userEmail;
    
    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    
    @Column(nullable = false)
    private String entityType; // Product, Order, User, etc.
    
    private Long entityId;
    
    @Column(length = 1000)
    private String details;
    
    private String ipAddress;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
}
```

### Bước 2: Tạo Aspect cho Auto-logging

**File mới:** `src/main/java/t4m/toy_store/audit/aspect/AuditAspect.java`

```java
package t4m.toy_store.audit.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import t4m.toy_store.audit.entity.AuditLog;
import t4m.toy_store.audit.repository.AuditLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    
    private final AuditLogRepository auditLogRepository;
    
    @AfterReturning(
        pointcut = "@annotation(org.springframework.web.bind.annotation.PostMapping) && " +
                   "within(t4m.toy_store.admin.controller..*)",
        returning = "result"
    )
    public void logAdminCreate(JoinPoint joinPoint, Object result) {
        logAuditAction(joinPoint, "CREATE", result);
    }
    
    @AfterReturning(
        pointcut = "@annotation(org.springframework.web.bind.annotation.PutMapping) && " +
                   "within(t4m.toy_store.admin.controller..*)",
        returning = "result"
    )
    public void logAdminUpdate(JoinPoint joinPoint, Object result) {
        logAuditAction(joinPoint, "UPDATE", result);
    }
    
    @AfterReturning(
        pointcut = "@annotation(org.springframework.web.bind.annotation.DeleteMapping) && " +
                   "within(t4m.toy_store.admin.controller..*)",
        returning = "result"
    )
    public void logAdminDelete(JoinPoint joinPoint, Object result) {
        logAuditAction(joinPoint, "DELETE", result);
    }
    
    private void logAuditAction(JoinPoint joinPoint, String action, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth != null ? auth.getName() : "anonymous";
            
            HttpServletRequest request = ((ServletRequestAttributes) 
                RequestContextHolder.currentRequestAttributes()).getRequest();
            String ipAddress = request.getRemoteAddr();
            
            String className = joinPoint.getSignature().getDeclaringTypeName();
            String methodName = joinPoint.getSignature().getName();
            
            AuditLog auditLog = AuditLog.builder()
                    .userEmail(userEmail)
                    .action(action)
                    .entityType(extractEntityType(className))
                    .details(String.format("%s.%s", className, methodName))
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();
            
            auditLogRepository.save(auditLog);
            log.info("Audit Log: {} - {} - {}", userEmail, action, auditLog.getEntityType());
            
        } catch (Exception e) {
            log.error("Failed to log audit action", e);
        }
    }
    
    private String extractEntityType(String className) {
        if (className.contains("Product")) return "Product";
        if (className.contains("Order")) return "Order";
        if (className.contains("User")) return "User";
        return "Unknown";
    }
}
```

---

## ✅ DEPLOYMENT CHECKLIST

### Before Production
```bash
# 1. Update application.properties for production
spring.profiles.active=prod

# 2. Enable HTTPS
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-alias=tomcat

# 3. Update CORS for production domain
cors.allowed-origins=https://yourdomain.com

# 4. Secure JWT secret
jwt.secret=${JWT_SECRET_FROM_ENV}

# 5. Database connection with SSL
spring.datasource.url=jdbc:mysql://localhost:3306/toystore?useSSL=true

# 6. Logging configuration
logging.level.t4m.toy_store=INFO
logging.level.org.springframework.security=WARN
```

### Testing Commands
```bash
# Test rate limiting
for i in {1..10}; do curl -X POST http://localhost:8080/api/auth/login; done

# Test authorization
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/orders/ORD-123

# Test refresh token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token>"}'
```

---

**Lưu ý:** Implement từng feature một và test kỹ trước khi deploy production!
