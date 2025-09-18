package t4m.toy_store.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}