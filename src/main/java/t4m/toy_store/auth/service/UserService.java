package t4m.toy_store.auth.service;

import t4m.toy_store.auth.dto.RegisterRequest;
import t4m.toy_store.auth.entity.Role;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.repository.RoleRepository;
import t4m.toy_store.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest dto) {
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        if (!dto.getRole().equals("ROLE_USER")) {
            throw new RuntimeException("Only ROLE_USER is allowed for registration");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswd(passwordEncoder.encode(dto.getPassword()));
        user.setActivated(true);

        Role userRole = roleRepository.findByRname(dto.getRole()).orElseThrow(() -> new RuntimeException("Role not found: " + dto.getRole()));
        user.getRoles().add(userRole);

        userRepository.save(user);
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(password, user.getPasswd())) {
            throw new RuntimeException("Invalid password");
        }
        if (!user.isActivated()) {
            throw new RuntimeException("Account not activated");
        }
        return user;
    }
}