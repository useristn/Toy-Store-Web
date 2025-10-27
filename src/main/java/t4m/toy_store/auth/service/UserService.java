package t4m.toy_store.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import t4m.toy_store.auth.dto.RegisterRequest;
import t4m.toy_store.auth.dto.UpdateProfileRequest;
import t4m.toy_store.auth.dto.UserProfileResponse;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.auth.util.JwtUtil;
import t4m.toy_store.auth.exception.*;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    private final Cache<String, Role> roleCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    /**
     * Register a new user with email and password.
     * All registrations are forced to ROLE_USER. Other roles must be created by admin.
     * Sends OTP for account activation.
     */
    public void register(RegisterRequest dto) {
        String sanitizedEmail = dto.getEmail().trim().toLowerCase();
        // Force all public registrations to ROLE_USER only
        String sanitizedRole = "ROLE_USER";

        logger.info("Attempting to register user with email: {} (role: {})", sanitizedEmail, sanitizedRole);

        if (userRepository.findByEmail(sanitizedEmail).isPresent()) {
            logger.warn("Registration failed: Email already exists - {}", sanitizedEmail);
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(sanitizedEmail);
        user.setPasswd(passwordEncoder.encode(dto.getPassword()));
        user.setActivated(false);

        Role userRole = roleCache.get(sanitizedRole, key -> roleRepository.findByRname(key)
                .orElseThrow(() -> {
                    logger.error("Registration failed: Role not found - {}", sanitizedRole);
                    return new InvalidRoleException("Role not found: " + sanitizedRole);
                }));
        user.getRoles().add(userRole);

        userRepository.save(user);
        logger.info("User registered successfully, pending activation: {}", sanitizedEmail);

        String otp = otpService.generateOtp();
        otpService.storeOtp(sanitizedEmail, otp, "activation");
        otpService.sendOtpEmail(sanitizedEmail, otp, "Account Activation");
    }

    /**
     * Send OTP for account activation.
     */
    public void sendActivationOtp(String email) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Sending activation OTP for email: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Activation OTP failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        if (user.isActivated()) {
            logger.warn("Account already activated: {}", sanitizedEmail);
            throw new AccountNotActivatedException("Account already activated");
        }

        String otp = otpService.generateOtp();
        otpService.storeOtp(sanitizedEmail, otp, "activation");
        otpService.sendOtpEmail(sanitizedEmail, otp, "Account Activation");
    }

    /**
     * Verify OTP and activate account.
     */
    public void verifyOtpAndActivate(String email, String otp) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting to verify OTP and activate account for email: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Activation failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        if (user.isActivated()) {
            logger.warn("Account already activated: {}", sanitizedEmail);
            throw new AccountNotActivatedException("Account already activated");
        }

        otpService.validateOtp(sanitizedEmail, otp, "activation");
        user.setActivated(true);
        userRepository.save(user);
        logger.info("Account activated successfully: {}", sanitizedEmail);
    }

    /**
     * Login user and generate JWT.
     */
    public String login(String email, String password) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting login for user: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Login failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        if (!passwordEncoder.matches(password, user.getPasswd())) {
            logger.warn("Login failed: Invalid password for user - {}", sanitizedEmail);
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!user.isActivated()) {
            logger.warn("Login failed: Account not activated - {}", sanitizedEmail);
            throw new AccountNotActivatedException("Account not activated");
        }

        try {
            String role = user.getRoles().stream()
                    .map(Role::getRname)
                    .findFirst()
                    .orElseThrow(() -> new InvalidRoleException("No role assigned to user"));
            String token = jwtUtil.generateToken(sanitizedEmail, Set.of(role));
            logger.info("Login successful for user: {}", sanitizedEmail);
            return token;
        } catch (Exception e) {
            logger.error("Error generating token for user {}: {}", sanitizedEmail, e.getMessage());
            throw new TokenGenerationException("Login failed due to token generation error");
        }
    }

    /**
     * Get user role.
     */
    public String getUserRole(String email) {
        String sanitizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("User not found for role retrieval: {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });
        return user.getRoles().stream()
                .map(Role::getRname)
                .findFirst()
                .orElseThrow(() -> new InvalidRoleException("No role assigned to user"));
    }

    /**
     * Send OTP for password reset.
     */
    public void sendForgotPasswordOtp(String email) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Sending forgot password OTP for email: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Forgot password failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        String otp = otpService.generateOtp();
        otpService.storeOtp(sanitizedEmail, otp, "forgot-password");
        otpService.sendOtpEmail(sanitizedEmail, otp, "Password Reset");
    }

    /**
     * Reset password with OTP.
     */
    public void resetPassword(String email, String otp, String newPassword) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting to reset password for email: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Reset password failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        otpService.validateOtp(sanitizedEmail, otp, "forgot-password");
        user.setPasswd(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password reset successfully for email: {}", sanitizedEmail);
    }

    /**
     * Get user profile information.
     */
    public UserProfileResponse getUserProfile(String email) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Fetching profile for user: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Profile fetch failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        List<String> roles = user.getRoles().stream()
                .map(Role::getRname)
                .collect(Collectors.toList());

        return new UserProfileResponse(user.getEmail(), user.getName(), user.getPhone(), user.getAddress(), roles);
    }

    /**
     * Update user profile information.
     */
    public void updateUserProfile(String email, UpdateProfileRequest dto) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting to update profile for user: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Profile update failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setAddress(dto.getAddress());
        userRepository.save(user);
        logger.info("Profile updated successfully for user: {}", sanitizedEmail);
    }

    /**
     * Change user password.
     * Validates current password before updating to new password.
     */
    public void changePassword(String email, String currentPassword, String newPassword) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting to change password for user: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail)
                .orElseThrow(() -> {
                    logger.warn("Password change failed: User not found - {}", sanitizedEmail);
                    return new UserNotFoundException("User not found");
                });

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswd())) {
            logger.warn("Password change failed: Invalid current password for user - {}", sanitizedEmail);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Update to new password
        user.setPasswd(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", sanitizedEmail);
    }
}