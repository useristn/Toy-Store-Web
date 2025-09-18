package t4m.toy_store.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.auth.dto.RegisterRequest;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.auth.util.JwtUtil;
import t4m.toy_store.auth.exception.*;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    private static final Set<String> ALLOWED_ROLES = new HashSet<>(Set.of("ROLE_USER", "ROLE_VENDOR", "ROLE_SHIPPER"));
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest dto) {
        String sanitizedEmail = dto.getEmail().trim().toLowerCase();
        String sanitizedRole = dto.getRole().trim();

        logger.info("Attempting to register user with email: {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));

        validateEmail(sanitizedEmail);
        validatePassword(dto.getPassword());
        validateRole(sanitizedRole);

        if (userRepository.findByEmail(sanitizedEmail).isPresent()) {
            logger.warn("Registration failed: Email already exists - {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(sanitizedEmail);
        user.setPasswd(passwordEncoder.encode(dto.getPassword()));
        user.setActivated(true); // OTP

        Role userRole = roleRepository.findByRname(sanitizedRole).orElseThrow(() -> {
            logger.error("Registration failed: Role not found - {}", sanitizedRole);
            return new InvalidRoleException("Role not found: " + sanitizedRole);
        });
        user.getRoles().add(userRole);

        userRepository.save(user);
        logger.info("User registered successfully: {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
    }

    public String login(String email, String password) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting login for user: {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));

        User user = userRepository.findByEmail(sanitizedEmail).orElseThrow(() -> {
            logger.warn("Login failed: User not found - {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
            return new UserNotFoundException("User not found");
        });

        if (!passwordEncoder.matches(password, user.getPasswd())) {
            logger.warn("Login failed: Invalid password for user - {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
            throw new InvalidCredentialsException("Invalid password");
        }

        if (!user.isActivated()) {
            logger.warn("Login failed: Account not activated - {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
            throw new AccountNotActivatedException("Account not activated");
        }

        try {
            Set<String> roles = user.getRoles().stream().map(Role::getRname).collect(Collectors.toSet());
            String token = jwtUtil.generateToken(sanitizedEmail, roles);
            logger.info("Login successful for user: {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
            return token;
        } catch (Exception e) {
            logger.error("Error generating token for user {}: {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"), e.getMessage());
            throw new RuntimeException("Login failed due to token generation error", e);
        }
    }

    public String getUserRole(String email) {
        String sanitizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(sanitizedEmail).orElseThrow(() -> {
            logger.warn("User not found for role retrieval: {}", sanitizedEmail.replaceAll("(\\w+?)@.*", "$1@***.com"));
            return new UserNotFoundException("User not found");
        });
        Optional<String> roleOpt = user.getRoles().stream().map(Role::getRname).findFirst();
        return roleOpt.orElseThrow(() -> new InvalidRoleException("No role assigned to user"));
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidRoleException("Invalid email format");
        }
    }

    private void validatePassword(String password) {
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new InvalidRoleException("Password must be strong: at least 8 chars, uppercase, lowercase, number, special char");
        }
    }

    private void validateRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new InvalidRoleException("Only ROLE_USER, ROLE_VENDOR, or ROLE_SHIPPER are allowed for registration");
        }
    }
}