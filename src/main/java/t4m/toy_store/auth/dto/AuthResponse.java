package t4m.toy_store.auth.dto;

import lombok.Data;

import java.util.Set;

@Data
public class AuthResponse {
    private Long userId;
    private String email;
    private Set<String> roles;
    private String message;
    private String token;

    public AuthResponse(Long userId, String email, Set<String> roles, String message) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
        this.message = message;
    }

    public AuthResponse(Long userId, String email, Set<String> roles, String message, String token) {
        this.userId = userId;
        this.email = email;
        this.roles = roles;
        this.message = message;
        this.token = token;
    }
}