package t4m.toy_store.support.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import t4m.toy_store.support.dto.ChatMessageDto;
import t4m.toy_store.support.service.SupportService;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class SupportChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final SupportService supportService;

    @MessageMapping("/support.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessage) {
        chatMessage.setCreatedAt(LocalDateTime.now());
        chatMessage.setMessageType("CHAT");

        // Save message to database
        supportService.saveMessage(chatMessage);

        // Send to specific session
        messagingTemplate.convertAndSend("/topic/support." + chatMessage.getSessionId(), chatMessage);

        // Notify admins if message is from user
        if ("USER".equals(chatMessage.getSenderType())) {
            messagingTemplate.convertAndSend("/topic/admin.notifications", chatMessage);
        }
    }

    @MessageMapping("/support.typing")
    public void typing(@Payload ChatMessageDto chatMessage) {
        chatMessage.setMessageType("TYPING");
        messagingTemplate.convertAndSend("/topic/support." + chatMessage.getSessionId(), chatMessage);
    }

    @MessageMapping("/support.join")
    public void joinChat(@Payload ChatMessageDto chatMessage) {
        chatMessage.setMessageType("JOIN");
        chatMessage.setCreatedAt(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/support." + chatMessage.getSessionId(), chatMessage);
    }

    @MessageMapping("/support.leave")
    public void leaveChat(@Payload ChatMessageDto chatMessage) {
        chatMessage.setMessageType("LEAVE");
        chatMessage.setCreatedAt(LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/support." + chatMessage.getSessionId(), chatMessage);
    }
}
