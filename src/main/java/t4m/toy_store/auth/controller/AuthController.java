package t4m.toy_store.auth.controller;

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
import t4m.toy_store.auth.dto.OtpRequest;
import t4m.toy_store.auth.dto.ResetPasswordRequest;
import t4m.toy_store.auth.service.UserService;
import t4m.toy_store.auth.dto.ErrorResponse;
import t4m.toy_store.auth.exception.*;

import java.util.HashMap;
import java.util.Map;

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
            return ResponseEntity.ok(new AuthResponse(dto.getEmail(), dto.getRole(), "Registration successful, please check your email for OTP"));
        } catch (EmailAlreadyExistsException | InvalidRoleException e) {
            logger.warn("Registration error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(dto.getEmail(), null, e.getMessage()));
        }
    }

    @PostMapping("/active-account")
    public ResponseEntity<AuthResponse> activeAccount(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            userService.sendActivationOtp(email);
            return ResponseEntity.ok(new AuthResponse(email, null, "OTP sent to your email for activation"));
        } catch (UserNotFoundException | AccountNotActivatedException | IllegalArgumentException e) {
            logger.warn("Activation OTP error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, null, null, e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody OtpRequest dto) {
        try {
            userService.verifyOtpAndActivate(dto.getEmail(), dto.getOtp());
            return ResponseEntity.ok(new AuthResponse(dto.getEmail(), null, "Account activated successfully"));
        } catch (UserNotFoundException | OtpInvalidException | OtpExpiredException | AccountNotActivatedException e) {
            logger.warn("OTP verification error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(dto.getEmail(), null, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest dto) {
        try {
            String token = userService.login(dto.getEmail(), dto.getPassword());
            String role = userService.getUserRole(dto.getEmail());
            return ResponseEntity.ok(new AuthResponse(dto.getEmail(), role, "Login successful", token));
        } catch (UserNotFoundException | InvalidCredentialsException | AccountNotActivatedException e) {
            logger.warn("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(dto.getEmail(), null, e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            if (email == null || email.trim().isEmpty()) {
                throw new IllegalArgumentException("Email is required");
            }
            userService.sendForgotPasswordOtp(email);
            return ResponseEntity.ok(new AuthResponse(email, null, "OTP sent to your email"));
        } catch (UserNotFoundException | IllegalArgumentException e) {
            logger.warn("Forgot password error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(null, null, null, e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest dto) {
        try {
            userService.resetPassword(dto.getEmail(), dto.getOtp(), dto.getNewPassword());
            return ResponseEntity.ok(new AuthResponse(dto.getEmail(), null, "Password reset successfully"));
        } catch (UserNotFoundException | OtpInvalidException | OtpExpiredException e) {
            logger.warn("Reset password error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthResponse(dto.getEmail(), null, e.getMessage()));
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