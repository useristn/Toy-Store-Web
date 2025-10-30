package t4m.toy_store.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.admin.dto.AccountCreateRequest;
import t4m.toy_store.admin.dto.AccountDTO;
import t4m.toy_store.admin.dto.AccountUpdateRequest;
import t4m.toy_store.admin.dto.BulkActionRequest;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.auth.service.EmailService;
import t4m.toy_store.order.repository.OrderRepository;
import t4m.toy_store.cart.repository.CartRepository;
import t4m.toy_store.favorite.repository.FavoriteRepository;
import t4m.toy_store.rating.repository.RatingRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAccountService {
    private static final Logger logger = LoggerFactory.getLogger(AdminAccountService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RatingRepository ratingRepository;

    /**
     * Get all accounts with pagination, search, and filtering
     */
    public Page<AccountDTO> getAccounts(String search, String roleFilter, String statusFilter,
                                         int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        // Get all users first (in production, use custom repository with Specifications)
        List<User> allUsers = userRepository.findAll();

        // Apply filters manually
        List<User> filteredUsers = allUsers.stream()
            .filter(user -> {
                // Search filter
                if (!search.isEmpty()) {
                    String searchLower = search.toLowerCase();
                    boolean matchesSearch = user.getName().toLowerCase().contains(searchLower) ||
                                          user.getEmail().toLowerCase().contains(searchLower) ||
                                          (user.getPhone() != null && user.getPhone().contains(search));
                    if (!matchesSearch) return false;
                }

                // Role filter
                if (!roleFilter.isEmpty()) {
                    boolean hasRole = user.getRoles().stream()
                        .anyMatch(role -> role.getRname().equals(roleFilter));
                    if (!hasRole) return false;
                }

                // Status filter (removed since we don't use ban/unban anymore)
                // if (!statusFilter.isEmpty()) {
                //     boolean statusMatch = statusFilter.equals("active") ? user.isActivated() : !user.isActivated();
                //     if (!statusMatch) return false;
                // }

                return true;
            })
            .sorted((u1, u2) -> {
                int comparison = 0;
                switch (sortBy) {
                    case "name":
                        comparison = u1.getName().compareTo(u2.getName());
                        break;
                    case "email":
                        comparison = u1.getEmail().compareTo(u2.getEmail());
                        break;
                    case "created":
                        comparison = u1.getCreated().compareTo(u2.getCreated());
                        break;
                    default:
                        comparison = u1.getCreated().compareTo(u2.getCreated());
                }
                return sortDir.equalsIgnoreCase("desc") ? -comparison : comparison;
            })
            .collect(Collectors.toList());

        // Manual pagination
        int totalItems = filteredUsers.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, totalItems);
        List<User> pageUsers = startIndex < totalItems ?
            filteredUsers.subList(startIndex, endIndex) : new ArrayList<>();

        // Convert to DTOs
        List<AccountDTO> pageDTOs = pageUsers.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        return new PageImpl<>(pageDTOs, pageable, totalItems);
    }

    /**
     * Get account by ID
     */
    public AccountDTO getAccountById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));
        return convertToDTO(user);
    }

    /**
     * Create new account
     */
    @Transactional
    public AccountDTO createAccount(AccountCreateRequest request) {
        // Validate
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        // Check if email exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswd(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setActivated(true); // Admin-created accounts are auto-activated
        user.setCreated(LocalDateTime.now());
        user.setUpdated(LocalDateTime.now());

        // Assign role
        Role role = roleRepository.findByRname(request.getRole())
            .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ: " + request.getRole()));
        user.setRoles(new HashSet<>(Collections.singletonList(role)));

        User savedUser = userRepository.save(user);
        logger.info("Admin created new account: {}", savedUser.getEmail());

        // Send welcome email
        try {
            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName(), "https://t4m.com/login");
        } catch (Exception e) {
            logger.error("Failed to send welcome email: {}", e.getMessage());
        }

        return convertToDTO(savedUser);
    }

    /**
     * Update account
     */
    @Transactional
    public AccountDTO updateAccount(Long id, AccountUpdateRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        // Update basic info
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUpdated(LocalDateTime.now());

        // Update role if provided
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            Role role = roleRepository.findByRname(request.getRole())
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ: " + request.getRole()));
            user.setRoles(new HashSet<>(Collections.singletonList(role)));
        }

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new RuntimeException("Mật khẩu xác nhận không khớp");
            }
            if (request.getPassword().length() < 6) {
                throw new RuntimeException("Mật khẩu phải có ít nhất 6 ký tự");
            }
            user.setPasswd(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        logger.info("Admin updated account: {}", updatedUser.getEmail());

        return convertToDTO(updatedUser);
    }

    /**
     * Delete account (permanent)
     * Warning: This will delete all related data (cart, favorites, ratings)
     * but will NOT delete orders to maintain data integrity
     */
    @Transactional
    public void deleteAccount(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        // Check if user has orders - DO NOT allow deletion if there are orders
        long orderCount = orderRepository.findByUserIdOrderByCreatedAtDesc(id).size();
        if (orderCount > 0) {
            throw new RuntimeException("Không thể xóa tài khoản này vì có " + orderCount + 
                " đơn hàng liên quan. Để bảo toàn dữ liệu đơn hàng, vui lòng không xóa tài khoản này.");
        }

        // Delete related data in correct order to avoid constraint violations
        logger.info("Deleting related data for user: {}", user.getEmail());
        
        // 1. Delete cart items (if exists)
        cartRepository.findByUserId(id).ifPresent(cart -> {
            cartRepository.delete(cart);
            logger.info("Deleted cart for user: {}", user.getEmail());
        });

        // 2. Delete favorites
        long favoriteCount = favoriteRepository.countByUserId(id);
        if (favoriteCount > 0) {
            favoriteRepository.findByUserIdOrderByCreatedAtDesc(id)
                .forEach(favorite -> favoriteRepository.delete(favorite));
            logger.info("Deleted {} favorites for user: {}", favoriteCount, user.getEmail());
        }

        // 3. Delete ratings (if user has no orders, they shouldn't have ratings, but check anyway)
        // Note: Ratings are typically tied to orders, so this should be empty
        
        // 4. Finally delete the user
        userRepository.delete(user);
        logger.info("Admin deleted account: {}", user.getEmail());
    }

    /**
     * Ban account (deactivate)
     */
    @Transactional
    public void banAccount(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        user.setActivated(false);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Admin banned account: {}", user.getEmail());
    }

    /**
     * Unban account (activate)
     */
    @Transactional
    public void unbanAccount(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        user.setActivated(true);
        user.setUpdated(LocalDateTime.now());
        userRepository.save(user);
        logger.info("Admin unbanned account: {}", user.getEmail());
    }

    /**
     * Reset password (send email)
     */
    @Transactional
    public void resetPassword(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với ID: " + id));

        // Generate reset token (in production, use proper token generation)
        String resetToken = UUID.randomUUID().toString();
        String resetLink = "https://t4m.com/reset-password?token=" + resetToken;

        // Send reset password email
        emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
        logger.info("Admin triggered password reset for account: {}", user.getEmail());
    }

    /**
     * Bulk actions
     */
    @Transactional
    public void bulkAction(BulkActionRequest request) {
        List<User> users = userRepository.findAllById(request.getUserIds());

        switch (request.getAction().toLowerCase()) {
            case "ban":
                users.forEach(user -> {
                    user.setActivated(false);
                    user.setUpdated(LocalDateTime.now());
                });
                userRepository.saveAll(users);
                logger.info("Admin banned {} accounts", users.size());
                break;

            case "unban":
                users.forEach(user -> {
                    user.setActivated(true);
                    user.setUpdated(LocalDateTime.now());
                });
                userRepository.saveAll(users);
                logger.info("Admin unbanned {} accounts", users.size());
                break;

            case "delete":
                userRepository.deleteAll(users);
                logger.info("Admin deleted {} accounts", users.size());
                break;

            default:
                throw new RuntimeException("Hành động không hợp lệ: " + request.getAction());
        }
    }

    /**
     * Search accounts
     */
    public List<AccountDTO> searchAccounts(String keyword) {
        List<User> users = userRepository.findAll();
        
        return users.stream()
            .filter(user -> 
                user.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                user.getEmail().toLowerCase().contains(keyword.toLowerCase()) ||
                (user.getPhone() != null && user.getPhone().contains(keyword))
            )
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert User entity to AccountDTO
     */
    private AccountDTO convertToDTO(User user) {
        AccountDTO dto = new AccountDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setName(user.getName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setActivated(user.isActivated());
        dto.setCreated(user.getCreated());
        dto.setUpdated(user.getUpdated());
        
        List<String> roleNames = user.getRoles().stream()
            .map(Role::getRname)
            .collect(Collectors.toList());
        dto.setRoles(roleNames);
        
        return dto;
    }
}
