package t4m.toy_store.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.auth.dto.AuthResponse;
import t4m.toy_store.auth.dto.LoginRequest;
import t4m.toy_store.auth.dto.RegisterRequest;
import t4m.toy_store.auth.service.UserService;
import t4m.toy_store.auth.dto.ErrorResponse;
import t4m.toy_store.auth.exception.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest dto) {
        try {
            userService.register(dto);
            return ResponseEntity.ok(new AuthResponse(null, dto.getEmail(), Set.of(dto.getRole()), "Registration successful"));
        } catch (EmailAlreadyExistsException | InvalidRoleException e) {
            logger.warn("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, dto.getEmail(), null, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest dto) {
        try {
            String token = userService.login(dto.getEmail(), dto.getPassword());
            return ResponseEntity.ok(new AuthResponse(null, dto.getEmail(), null, "Login successful", token));
        } catch (UserNotFoundException | InvalidCredentialsException | AccountNotActivatedException e) {
            logger.warn("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, dto.getEmail(), null, e.getMessage()));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        logger.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), errors.toString()));
    }
}