package t4m.toy_store.infra.websocket;

//# REST + @MessageMapping endpoints
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.sendPublic")
    public void sendPublic(@Payload ChatMessage message) {
        message.setRoomId("public");
        chatService.sendMessage(message);
    }

    @MessageMapping("/chat.sendPrivate")
    public void sendPrivate(@Payload ChatMessage message) {
        chatService.sendMessage(message);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getSenderId());
        chatService.sendMessage(message);
    }

//    @MessageMapping("/chat.admin")
//    public void sendAdminMessage(@Payload ChatMessage message) {
//        // Kiểm tra role
//        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities()
//                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
//            throw new AccessDeniedException("Not allowed");
//        }
//        message.setType(ChatMessage.MessageType.CHAT);
//        chatService.sendMessageToRoom("global-announcement", message);
//    }
}