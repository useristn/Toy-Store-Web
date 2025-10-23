package t4m.toy_store.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import t4m.toy_store.chatbot.dto.ChatbotRequest;
import t4m.toy_store.chatbot.dto.ChatbotResponse;
import t4m.toy_store.chatbot.service.ChatbotService;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);
    
    private final ChatbotService chatbotService;
    
    /**
     * Handle chatbot messages - public endpoint (no auth required for better UX)
     * Customers can ask questions without logging in
     */
    @PostMapping("/message")
    public ResponseEntity<ChatbotResponse> handleMessage(@RequestBody ChatbotRequest request) {
        try {
            logger.info("Received chatbot message: {}", request.getMessage());
            
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ChatbotResponse.error("Vui lòng nhập câu hỏi của bạn! 😊"));
            }
            
            // Generate conversation ID if not provided
            String conversationId = request.getConversationId();
            if (conversationId == null || conversationId.isEmpty()) {
                conversationId = chatbotService.generateConversationId();
            }
            
            // Get AI response
            String aiReply = chatbotService.generateResponse(request.getMessage(), conversationId);
            
            return ResponseEntity.ok(ChatbotResponse.success(aiReply, conversationId));
            
        } catch (Exception e) {
            logger.error("Error handling chatbot message", e);
            return ResponseEntity.internalServerError()
                .body(ChatbotResponse.error("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau! 😊"));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot service is running! 🤖");
    }
}
