package t4m.toy_store.auth.dto;

import lombok.Data;

@Data
public class ErrorResponse {
    private int status;
    private String error;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
    }
}