package t4m.toy_store.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(min = 5, max = 50, message = "Name must be between 5 and 50 characters")
    private String name;

    @Pattern(regexp = "^0\\d{9}$", message = "Invalid phone number")
    private String phone;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;
}