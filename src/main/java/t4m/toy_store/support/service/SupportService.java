package t4m.toy_store.support.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import t4m.toy_store.support.dto.ChatMessageDto;
import t4m.toy_store.support.dto.SupportSessionDto;
import t4m.toy_store.support.entity.SupportMessage;
import t4m.toy_store.support.entity.SupportSession;
import t4m.toy_store.support.repository.SupportMessageRepository;
import t4m.toy_store.support.repository.SupportSessionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportService {

    private final SupportSessionRepository sessionRepository;
    private final SupportMessageRepository messageRepository;

    @Transactional
    public SupportSession createOrGetSession(Long userId, String userEmail, String userName) {
        return sessionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    SupportSession session = new SupportSession();
                    session.setSessionId(UUID.randomUUID().toString());
                    session.setUserId(userId);
                    session.setUserEmail(userEmail);
                    session.setUserName(userName);
                    session.setStatus("ACTIVE");
                    return sessionRepository.save(session);
                });
    }

    @Transactional
    public SupportMessage saveMessage(ChatMessageDto messageDto) {
        SupportMessage message = new SupportMessage();
        message.setSessionId(messageDto.getSessionId());
        message.setUserId(messageDto.getUserId());
        message.setUserEmail(messageDto.getUserEmail());
        message.setUserName(messageDto.getUserName());
        message.setSenderType(messageDto.getSenderType());
        message.setMessage(messageDto.getMessage());
        message.setCreatedAt(LocalDateTime.now());

        SupportMessage saved = messageRepository.save(message);

        // Update session
        sessionRepository.findBySessionId(messageDto.getSessionId())
                .ifPresent(session -> {
                    session.setUpdatedAt(LocalDateTime.now());
                    if ("USER".equals(messageDto.getSenderType())) {
                        session.setUnreadCount(session.getUnreadCount() + 1);
                    }
                    sessionRepository.save(session);
                });

        return saved;
    }

    public List<SupportMessage> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public List<SupportSessionDto> getAllActiveSessions() {
        List<SupportSession> sessions = sessionRepository.findByStatusOrderByUpdatedAtDesc("ACTIVE");
        return sessions.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(String sessionId, String senderType) {
        List<SupportMessage> messages = messageRepository.findBySessionIdAndIsReadFalse(sessionId);
        messages.stream()
                .filter(msg -> !msg.getSenderType().equals(senderType))
                .forEach(msg -> msg.setRead(true));
        messageRepository.saveAll(messages);

        // Reset unread count
        sessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    session.setUnreadCount(0);
                    sessionRepository.save(session);
                });
    }

    @Transactional
    public void closeSession(String sessionId) {
        sessionRepository.findBySessionId(sessionId)
                .ifPresent(session -> {
                    session.setStatus("CLOSED");
                    sessionRepository.save(session);
                });
    }

    public long getUnreadCount(String sessionId, String senderType) {
        return messageRepository.countBySessionIdAndSenderTypeAndIsReadFalse(sessionId, senderType);
    }

    private SupportSessionDto convertToDto(SupportSession session) {
        SupportSessionDto dto = new SupportSessionDto();
        dto.setId(session.getId());
        dto.setSessionId(session.getSessionId());
        dto.setUserId(session.getUserId());
        dto.setUserEmail(session.getUserEmail());
        dto.setUserName(session.getUserName());
        dto.setStatus(session.getStatus());
        dto.setCreatedAt(session.getCreatedAt());
        dto.setUpdatedAt(session.getUpdatedAt());
        dto.setUnreadCount(session.getUnreadCount());

        // Get last message
        List<SupportMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getSessionId());
        if (!messages.isEmpty()) {
            dto.setLastMessage(messages.get(messages.size() - 1).getMessage());
        }

        return dto;
    }
}
