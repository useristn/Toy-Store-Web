package t4m.toy_store.auth.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String email;
    private String role;
    private String message;
    private String token;

    public AuthResponse(String email, String role, String message) {
        this.email = email;
        this.role = role;
        this.message = message;
    }

    public AuthResponse(String email, String role, String message, String token) {
        this.email = email;
        this.role = role;
        this.message = message;
        this.token = token;
    }
}