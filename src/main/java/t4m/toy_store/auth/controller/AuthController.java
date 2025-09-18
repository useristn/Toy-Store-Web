package t4m.toy_store.controller;

import t4m.toy_store.auth.dto.LoginRequest;
import t4m.toy_store.auth.dto.RegisterRequest;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.auth.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest dto) {
        try {
            userService.register(dto);
            return ResponseEntity.ok("Registration successful");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest dto) {
        try {
            User user = userService.login(dto.getEmail(), dto.getPassword());
            return ResponseEntity.ok("Login successful for " + user.getEmail() + "Role: " + user.getRoles());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}