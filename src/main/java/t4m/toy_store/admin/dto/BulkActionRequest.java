package t4m.toy_store.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class BulkActionRequest {
    private List<Long> userIds;
    private String action; // "ban", "unban", "delete"
}
