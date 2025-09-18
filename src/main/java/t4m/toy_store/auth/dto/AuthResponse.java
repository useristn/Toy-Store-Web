package t4m.toy_store.auth.dto;

import lombok.Data;

import java.util.Set;

@Data
public class AuthResponse {
    private String email;
    private Set<String> roles;
    private String message;

    public AuthResponse(String email, Set<String> roles, String message) {
        this.email = email;
        this.roles = roles;
        this.message = message;
    }
}