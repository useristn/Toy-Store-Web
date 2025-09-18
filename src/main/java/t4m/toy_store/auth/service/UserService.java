package t4m.toy_store.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t4m.toy_store.auth.dto.RegisterRequest;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;
import t4m.toy_store.auth.util.JwtUtil;
import t4m.toy_store.auth.exception.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private static final Set<String> ALLOWED_ROLES = new HashSet<>(Set.of("ROLE_USER", "ROLE_VENDOR", "ROLE_SHIPPER"));

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public void register(RegisterRequest dto) {
        String sanitizedEmail = dto.getEmail().trim().toLowerCase();
        String sanitizedRole = dto.getRole().trim();

        logger.info("Attempting to register user with email: {}", sanitizedEmail);

        if (userRepository.findByEmail(sanitizedEmail).isPresent()) {
            logger.warn("Registration failed: Email already exists - {}", sanitizedEmail);
            throw new EmailAlreadyExistsException("Email already exists");
        }

        if (!ALLOWED_ROLES.contains(sanitizedRole)) {
            logger.warn("Registration failed: Invalid role - {}", sanitizedRole);
            throw new InvalidRoleException("Only ROLE_USER, ROLE_VENDOR, or ROLE_SHIPPER are allowed for registration");
        }

        User user = new User();
        user.setEmail(sanitizedEmail);
        user.setPasswd(passwordEncoder.encode(dto.getPassword()));
        user.setActivated(true);

        Role userRole = roleRepository.findByRname(sanitizedRole).orElseThrow(() -> {
            logger.error("Registration failed: Role not found - {}", sanitizedRole);
            return new InvalidRoleException("Role not found: " + sanitizedRole);
        });
        user.getRoles().add(userRole);

        userRepository.save(user);
        logger.info("User registered successfully: {}", sanitizedEmail);
    }

    public String login(String email, String password) {
        String sanitizedEmail = email.trim().toLowerCase();
        logger.info("Attempting login for user: {}", sanitizedEmail);

        User user = userRepository.findByEmail(sanitizedEmail).orElseThrow(() -> {
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

        Set<String> roles = user.getRoles().stream().map(Role::getRname).collect(Collectors.toSet());
        String token = jwtUtil.generateToken(sanitizedEmail, roles);
        logger.info("Login successful for user: {}", sanitizedEmail);
        return token;
    }
}