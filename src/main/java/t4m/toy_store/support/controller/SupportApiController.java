package t4m.toy_store.support.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.auth.entity.User;
import t4m.toy_store.support.dto.SupportSessionDto;
import t4m.toy_store.support.entity.SupportMessage;
import t4m.toy_store.support.entity.SupportSession;
import t4m.toy_store.support.service.SupportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportApiController {

    private final SupportService supportService;

    @PostMapping("/session")
    public ResponseEntity<?> createSession(@AuthenticationPrincipal User user) {
        try {
            SupportSession session = supportService.createOrGetSession(
                    user.getId(),
                    user.getEmail(),
                    user.getName());
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String sessionId) {
        try {
            List<SupportMessage> messages = supportService.getSessionMessages(sessionId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/session/{sessionId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable String sessionId,
            @RequestParam String senderType) {
        try {
            supportService.markMessagesAsRead(sessionId, senderType);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/admin/sessions")
    public ResponseEntity<?> getAllSessions() {
        try {
            List<SupportSessionDto> sessions = supportService.getAllActiveSessions();
            return ResponseEntity.ok(sessions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/admin/session/{sessionId}/close")
    public ResponseEntity<?> closeSession(@PathVariable String sessionId) {
        try {
            supportService.closeSession(sessionId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/session/{sessionId}/unread")
    public ResponseEntity<?> getUnreadCount(
            @PathVariable String sessionId,
            @RequestParam String senderType) {
        try {
            long count = supportService.getUnreadCount(sessionId, senderType);
            Map<String, Object> response = new HashMap<>();
            response.put("unreadCount", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
