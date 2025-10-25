package t4m.toy_store.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // Check if it's an API request
        String requestUri = request.getRequestURI();
        if (requestUri.startsWith("/api/")) {
            // Return JSON response for API requests
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");

            Map<String, String> error = new HashMap<>();
            error.put("error", "Access Denied");
            error.put("message", "Bạn không có quyền truy cập vào tài nguyên này");

            response.getWriter().write(objectMapper.writeValueAsString(error));
        } else {
            // For HTML pages (including /admin/**), redirect to login with error
            response.sendRedirect("/login?error=access_denied");
        }
    }
}
