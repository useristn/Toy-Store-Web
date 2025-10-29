package t4m.toy_store.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import t4m.toy_store.chatbot.dto.IntentClassification;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step 2: NLP & Intent Recognition
 * Xử lý ngôn ngữ tự nhiên và nhận diện mục đích + thông số
 */
@Service
@RequiredArgsConstructor
public class IntentRecognitionService {
    private static final Logger logger = LoggerFactory.getLogger(IntentRecognitionService.class);
    
    // Keyword patterns for intent classification
    private static final Map<IntentClassification.Intent, String[]> INTENT_KEYWORDS = new HashMap<>();
    
    static {
        INTENT_KEYWORDS.put(IntentClassification.Intent.PRODUCT_RECOMMENDATION, 
            new String[]{"tư vấn", "gợi ý", "đề xuất", "nên mua", "phù hợp", "chọn gì"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.PRODUCT_SEARCH,
            new String[]{"tìm", "có không", "bán", "có bán", "shop có"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.PRICE_INQUIRY,
            new String[]{"giá", "bao nhiêu", "tiền", "cost", "price"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.AVAILABILITY_CHECK,
            new String[]{"còn hàng", "còn không", "available", "hết hàng", "stock"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.POLICY_INQUIRY,
            new String[]{"chính sách", "đổi trả", "giao hàng", "ship", "hoàn tiền", "bảo hành"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.AGE_RECOMMENDATION,
            new String[]{"tuổi", "bé", "cháu", "con", "years old", "tháng"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.PROMOTION_INQUIRY,
            new String[]{"khuyến mãi", "giảm giá", "sale", "promotion", "discount", "voucher"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.ORDER_TRACKING,
            new String[]{"đơn hàng", "order", "tracking", "theo dõi", "ship đâu"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.COMPLAINT,
            new String[]{"khiếu nại", "complain", "tệ", "kém", "không hài lòng", "thất vọng"});
        
        INTENT_KEYWORDS.put(IntentClassification.Intent.HUMAN_HANDOFF,
            new String[]{"nhân viên", "staff", "người thật", "gặp", "liên hệ", "hotline"});
    }
    
    /**
     * Classify intent from user message
     */
    public IntentClassification classifyIntent(String message, String conversationContext) {
        IntentClassification classification = new IntentClassification();
        String lowerMsg = message.toLowerCase().trim();
        
        // Score each intent
        Map<IntentClassification.Intent, Double> scores = new HashMap<>();
        
        for (Map.Entry<IntentClassification.Intent, String[]> entry : INTENT_KEYWORDS.entrySet()) {
            double score = calculateIntentScore(lowerMsg, entry.getValue());
            if (score > 0) {
                scores.put(entry.getKey(), score);
            }
        }
        
        // Get highest score
        IntentClassification.Intent bestIntent = IntentClassification.Intent.GENERAL_INQUIRY;
        double bestScore = 0.0;
        
        for (Map.Entry<IntentClassification.Intent, Double> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestIntent = entry.getKey();
            }
        }
        
        // If no strong intent, check context
        if (bestScore < 0.3 && conversationContext != null) {
            bestIntent = inferFromContext(conversationContext);
            bestScore = 0.5;
        }
        
        classification.setIntent(bestIntent);
        classification.setConfidence(Math.min(1.0, bestScore));
        
        // Extract slots
        extractSlots(lowerMsg, classification);
        
        logger.info("Intent classified: {} (confidence: {})", bestIntent, bestScore);
        
        return classification;
    }
    
    /**
     * Calculate intent score based on keyword matching
     */
    private double calculateIntentScore(String message, String[] keywords) {
        double score = 0.0;
        for (String keyword : keywords) {
            if (message.contains(keyword)) {
                score += 0.2; // Each keyword adds 0.2
            }
        }
        return score;
    }
    
    /**
     * Infer intent from conversation context
     */
    private IntentClassification.Intent inferFromContext(String context) {
        if (context.contains("tuổi") || context.contains("bé")) {
            return IntentClassification.Intent.AGE_RECOMMENDATION;
        }
        if (context.contains("category") || context.contains("danh mục")) {
            return IntentClassification.Intent.CATEGORY_BROWSE;
        }
        return IntentClassification.Intent.PRODUCT_RECOMMENDATION;
    }
    
    /**
     * Extract slots (parameters) from message
     */
    private void extractSlots(String message, IntentClassification classification) {
        // Extract child gender
        if (message.contains("con gái") || message.contains("bé gái") || message.contains("cháu gái")) {
            classification.setSlot(IntentClassification.SLOT_CHILD_GENDER, "girl");
        } else if (message.contains("con trai") || message.contains("bé trai") || message.contains("cháu trai")) {
            classification.setSlot(IntentClassification.SLOT_CHILD_GENDER, "boy");
        }
        
        // Extract age
        Pattern agePattern = Pattern.compile("(\\d+)\\s*(tuổi|years?|tháng|months?)");
        Matcher ageMatcher = agePattern.matcher(message);
        if (ageMatcher.find()) {
            try {
                int age = Integer.parseInt(ageMatcher.group(1));
                String unit = ageMatcher.group(2);
                
                // Convert months to years if needed
                if (unit.contains("tháng") || unit.contains("month")) {
                    age = age / 12; // Simple conversion
                }
                
                classification.setSlot(IntentClassification.SLOT_CHILD_AGE, age);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse age from: {}", ageMatcher.group(1));
            }
        }
        
        // Extract category keywords
        if (message.contains("búp bê") || message.contains("doll")) {
            classification.setSlot(IntentClassification.SLOT_CATEGORY, "búp bê");
        } else if (message.contains("xe") || message.contains("car")) {
            classification.setSlot(IntentClassification.SLOT_CATEGORY, "xe cộ");
        } else if (message.contains("lego") || message.contains("xếp hình")) {
            classification.setSlot(IntentClassification.SLOT_CATEGORY, "xếp hình");
        } else if (message.contains("robot")) {
            classification.setSlot(IntentClassification.SLOT_CATEGORY, "robot");
        }
        
        // Extract price range
        if (message.contains("rẻ") || message.contains("giá thấp") || message.contains("cheap")) {
            classification.setSlot(IntentClassification.SLOT_PRICE_RANGE, "low");
        } else if (message.contains("cao cấp") || message.contains("đắt") || message.contains("premium")) {
            classification.setSlot(IntentClassification.SLOT_PRICE_RANGE, "high");
        }
        
        // Extract occasion
        if (message.contains("sinh nhật") || message.contains("birthday")) {
            classification.setSlot(IntentClassification.SLOT_OCCASION, "birthday");
        } else if (message.contains("tết") || message.contains("holiday") || message.contains("lễ")) {
            classification.setSlot(IntentClassification.SLOT_OCCASION, "holiday");
        }
    }
}
