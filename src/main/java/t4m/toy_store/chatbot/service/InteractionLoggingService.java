package t4m.toy_store.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import t4m.toy_store.chatbot.dto.ConversationState;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Step 5: Interaction Logging and Learning
 * Ghi lại và học từ tương tác để cải thiện chatbot
 */
@Service
@RequiredArgsConstructor
public class InteractionLoggingService {
    private static final Logger logger = LoggerFactory.getLogger(InteractionLoggingService.class);
    
    // In-memory analytics (in production, use database or analytics service)
    private final Map<String, ConversationState> conversationStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> intentFrequency = new ConcurrentHashMap<>();
    private final Map<String, Integer> productClickThroughs = new ConcurrentHashMap<>();
    
    /**
     * Log conversation state
     */
    public void logConversation(String conversationId, ConversationState state) {
        conversationStates.put(conversationId, state);
        logger.info("Conversation logged: {} (Stage: {}, Messages: {})", 
                    conversationId, state.getCurrentStage(), state.getMessageCount());
    }
    
    /**
     * Get or create conversation state
     */
    public ConversationState getOrCreateState(String conversationId) {
        return conversationStates.computeIfAbsent(conversationId, id -> {
            ConversationState state = new ConversationState();
            state.setConversationId(id);
            state.setStartTime(LocalDateTime.now());
            state.setLastUpdateTime(LocalDateTime.now());
            state.setCurrentStage(ConversationState.ConversationStage.GREETING);
            state.setMessageCount(0);
            return state;
        });
    }
    
    /**
     * Update conversation state
     */
    public void updateState(String conversationId, ConversationState.ConversationStage stage) {
        ConversationState state = getOrCreateState(conversationId);
        state.setCurrentStage(stage);
        state.setLastUpdateTime(LocalDateTime.now());
        logConversation(conversationId, state);
    }
    
    /**
     * Log intent occurrence for analytics
     */
    public void logIntent(String intent) {
        intentFrequency.merge(intent, 1, Integer::sum);
        logger.debug("Intent logged: {} (Total: {})", intent, intentFrequency.get(intent));
    }
    
    /**
     * Log product click-through
     */
    public void logProductClickThrough(String conversationId, String productName) {
        ConversationState state = getOrCreateState(conversationId);
        state.recordClickThrough();
        productClickThroughs.merge(productName, 1, Integer::sum);
        
        logger.info("Product click-through: {} in conversation {}", productName, conversationId);
    }
    
    /**
     * Log successful recommendation
     */
    public void logSuccessfulRecommendation(String conversationId) {
        ConversationState state = getOrCreateState(conversationId);
        state.recordSuccess();
        logger.info("Successful recommendation in conversation: {}", conversationId);
    }
    
    /**
     * Log handoff request
     */
    public void logHandoffRequest(String conversationId, String reason) {
        ConversationState state = getOrCreateState(conversationId);
        state.setHandoffRequested(true);
        state.setHandoffReason(reason);
        logger.warn("Handoff requested for conversation {}: {}", conversationId, reason);
    }
    
    /**
     * Get conversation metrics for analytics
     */
    public Map<String, Object> getConversationMetrics(String conversationId) {
        ConversationState state = conversationStates.get(conversationId);
        if (state == null) {
            return Map.of("error", "Conversation not found");
        }
        
        long durationMinutes = Duration.between(state.getStartTime(), 
                                                state.getLastUpdateTime()).toMinutes();
        
        return Map.of(
            "conversationId", conversationId,
            "duration_minutes", durationMinutes,
            "message_count", state.getMessageCount(),
            "successful_recommendations", state.getSuccessfulRecommendations(),
            "click_throughs", state.getClickThroughs(),
            "current_stage", state.getCurrentStage().toString(),
            "handoff_requested", state.isHandoffRequested()
        );
    }
    
    /**
     * Get global analytics
     */
    public Map<String, Object> getGlobalAnalytics() {
        int totalConversations = conversationStates.size();
        int handoffRequests = (int) conversationStates.values().stream()
            .filter(ConversationState::isHandoffRequested)
            .count();
        
        int totalClickThroughs = productClickThroughs.values().stream()
            .mapToInt(Integer::intValue)
            .sum();
        
        return Map.of(
            "total_conversations", totalConversations,
            "handoff_requests", handoffRequests,
            "handoff_rate", totalConversations > 0 ? (double) handoffRequests / totalConversations : 0.0,
            "total_click_throughs", totalClickThroughs,
            "intent_frequency", intentFrequency,
            "top_products", productClickThroughs
        );
    }
    
    /**
     * Cleanup old conversations (memory management)
     */
    public void cleanupOldConversations(int maxAgeHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxAgeHours);
        
        conversationStates.entrySet().removeIf(entry -> 
            entry.getValue().getLastUpdateTime().isBefore(cutoff)
        );
        
        logger.info("Cleaned up conversations older than {} hours. Remaining: {}", 
                    maxAgeHours, conversationStates.size());
    }
}
