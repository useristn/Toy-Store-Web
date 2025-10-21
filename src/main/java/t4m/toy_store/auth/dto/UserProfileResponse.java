package t4m.toy_store.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserProfileResponse {
    private String email;
    private String name;
    private String phone;
    private String address;
    private List<String> roles;

    public UserProfileResponse(String email, String name, String phone, String address, List<String> roles) {
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.roles = roles;
    }
}