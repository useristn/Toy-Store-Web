package t4m.toy_store.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AccountDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String address;
    private boolean activated;
    private LocalDateTime created;
    private LocalDateTime updated;
    private List<String> roles;
}
