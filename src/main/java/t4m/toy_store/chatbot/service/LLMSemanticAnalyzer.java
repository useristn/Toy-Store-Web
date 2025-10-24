package t4m.toy_store.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import t4m.toy_store.chatbot.service.NLUService.Intent;
import t4m.toy_store.chatbot.service.NLUService.Language;

import java.util.*;

/**
 * LLM Semantic Analyzer - Deep semantic understanding layer
 * 
 * Role: LLM đứng sau NLU, chỉ can thiệp khi:
 * - Confidence score thấp (<0.75)
 * - Intent mơ hồ (UNKNOWN)
 * - Entity extraction khó
 * - Ngữ cảnh phức tạp
 * 
 * LLM KHÔNG trực tiếp trả lời user, chỉ phân tích và trả về:
 * - Intent (đề xuất)
 * - Entities (trích xuất)
 * - Keywords (gợi ý)
 * - Confidence (đánh giá lại)
 */
@Service
@RequiredArgsConstructor
public class LLMSemanticAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(LLMSemanticAnalyzer.class);
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // LLM semantic analysis result
    public static class SemanticAnalysis {
        private final Intent suggestedIntent;
        private final double confidence;
        private final Map<String, Object> extractedEntities;
        private final List<String> suggestedKeywords;
        private final String reasoning;
        
        public SemanticAnalysis(Intent intent, double confidence, 
                               Map<String, Object> entities, 
                               List<String> keywords,
                               String reasoning) {
            this.suggestedIntent = intent;
            this.confidence = confidence;
            this.extractedEntities = entities;
            this.suggestedKeywords = keywords;
            this.reasoning = reasoning;
        }
        
        public Intent getSuggestedIntent() { return suggestedIntent; }
        public double getConfidence() { return confidence; }
        public Map<String, Object> getExtractedEntities() { return extractedEntities; }
        public List<String> getSuggestedKeywords() { return suggestedKeywords; }
        public String getReasoning() { return reasoning; }
        
        @Override
        public String toString() {
            return String.format("SemanticAnalysis[intent=%s, conf=%.2f, entities=%s, keywords=%s]",
                    suggestedIntent, confidence, extractedEntities, suggestedKeywords);
        }
    }
    
    /**
     * Main method: Analyze ambiguous input with LLM
     * 
     * @param message User's message
     * @param language Detected language
     * @param nluIntent Initial NLU intent (may be UNKNOWN)
     * @param nluConfidence Initial NLU confidence
     * @return Semantic analysis from LLM
     */
    public SemanticAnalysis analyzeWithLLM(String message, Language language, 
                                          Intent nluIntent, double nluConfidence) {
        logger.info("🤖 LLM Semantic Analyzer invoked");
        logger.info("   Input: {}", message);
        logger.info("   Language: {}, NLU Intent: {}, NLU Confidence: {:.2f}", 
                   language, nluIntent, nluConfidence);
        
        if (geminiApiKey == null || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            logger.warn("Gemini API key not configured, returning fallback");
            return createFallbackAnalysis(nluIntent, nluConfidence);
        }
        
        try {
            // Build semantic analysis prompt
            String prompt = buildSemanticPrompt(message, language, nluIntent, nluConfidence);
            
            // Call Gemini API for semantic analysis
            String llmResponse = callGeminiForSemantics(prompt);
            
            // Parse LLM response
            SemanticAnalysis analysis = parseSemanticResponse(llmResponse, nluIntent);
            
            logger.info("✅ LLM Analysis: {}", analysis);
            return analysis;
            
        } catch (Exception e) {
            logger.error("❌ LLM semantic analysis failed: {}", e.getMessage(), e);
            return createFallbackAnalysis(nluIntent, nluConfidence);
        }
    }
    
    /**
     * Build prompt for LLM semantic analysis
     * LLM chỉ phân tích, KHÔNG trả lời trực tiếp
     */
    private String buildSemanticPrompt(String message, Language language, 
                                       Intent nluIntent, double nluConfidence) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Bạn là một Semantic Analyzer cho hệ thống chatbot đồ chơi trẻ em T4M.\n\n");
        prompt.append("NHIỆM VỤ: Phân tích ngữ nghĩa sâu của câu hỏi user, KHÔNG trả lời trực tiếp.\n\n");
        
        prompt.append("INPUT:\n");
        prompt.append("- User message: \"").append(message).append("\"\n");
        prompt.append("- Detected language: ").append(language).append("\n");
        prompt.append("- NLU initial intent: ").append(nluIntent).append("\n");
        prompt.append("- NLU confidence: ").append(String.format("%.2f", nluConfidence)).append("\n\n");
        
        prompt.append("PHÂN TÍCH:\n");
        prompt.append("1. Intent (ý định thực sự của user):\n");
        prompt.append("   Available intents: GREETING, PRICE_QUERY, PRODUCT_SEARCH, POLICY_QUERY, ");
        prompt.append("GIFT_CONSULTATION, COMPARISON, RECOMMENDATION, UNKNOWN\n\n");
        
        prompt.append("2. Entities (thông tin cần trích xuất):\n");
        prompt.append("   - price (số tiền, VD: 200k → 200000)\n");
        prompt.append("   - age (tuổi, VD: 5 tuổi → 5)\n");
        prompt.append("   - gender (giới tính: 'male'/'female')\n");
        prompt.append("   - quantity (số lượng)\n");
        prompt.append("   - minPrice, maxPrice (khoảng giá)\n\n");
        
        prompt.append("3. Keywords (từ khóa sản phẩm):\n");
        prompt.append("   Categories: búp bê, xe ô tô, lego, robot, thú bông, đồ chơi giáo dục, ");
        prompt.append("đồ chơi thể thao, nhà búp bê\n");
        prompt.append("   Brands: lego, barbie, disney, marvel, hasbro\n\n");
        
        prompt.append("4. Confidence (độ tin cậy 0-1):\n");
        prompt.append("   Đánh giá độ chắc chắn của phân tích\n\n");
        
        prompt.append("5. Reasoning (lý do):\n");
        prompt.append("   Giải thích ngắn gọn tại sao chọn intent này\n\n");
        
        prompt.append("OUTPUT FORMAT (JSON):\n");
        prompt.append("{\n");
        prompt.append("  \"intent\": \"<INTENT_NAME>\",\n");
        prompt.append("  \"confidence\": <0.0-1.0>,\n");
        prompt.append("  \"entities\": {\n");
        prompt.append("    \"age\": <number>,\n");
        prompt.append("    \"gender\": \"<male/female>\",\n");
        prompt.append("    \"price\": <number>,\n");
        prompt.append("    \"minPrice\": <number>,\n");
        prompt.append("    \"maxPrice\": <number>,\n");
        prompt.append("    \"quantity\": <number>\n");
        prompt.append("  },\n");
        prompt.append("  \"keywords\": [\"keyword1\", \"keyword2\"],\n");
        prompt.append("  \"reasoning\": \"<short explanation>\"\n");
        prompt.append("}\n\n");
        
        prompt.append("LƯU Ý:\n");
        prompt.append("- CHỈ trả về JSON, KHÔNG giải thích thêm\n");
        prompt.append("- Entities có thể rỗng {} nếu không tìm thấy\n");
        prompt.append("- Keywords có thể rỗng [] nếu không có\n");
        prompt.append("- Confidence cao (>0.8) nếu chắc chắn, thấp (<0.5) nếu mơ hồ\n");
        
        return prompt.toString();
    }
    
    /**
     * Call Gemini API for semantic analysis
     */
    private String callGeminiForSemantics(String prompt) throws Exception {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-exp:generateContent?key=" + geminiApiKey;
        
        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        
        // Add contents
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);
        
        // Add generation config for JSON response
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.1); // Low temperature for consistent analysis
        generationConfig.put("maxOutputTokens", 500); // Short response
        requestBody.put("generationConfig", generationConfig);
        
        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        logger.info("📤 Calling Gemini for semantic analysis...");
        ResponseEntity<String> response = restTemplate.exchange(
            apiUrl,
            HttpMethod.POST,
            entity,
            String.class
        );
        
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode contentNode = firstCandidate.path("content");
                JsonNode partsNode = contentNode.path("parts");
                
                if (partsNode.isArray() && partsNode.size() > 0) {
                    String text = partsNode.get(0).path("text").asText();
                    logger.info("📥 LLM response: {}", text);
                    return text;
                }
            }
        }
        
        throw new RuntimeException("Invalid Gemini API response");
    }
    
    /**
     * Parse LLM response to SemanticAnalysis
     */
    private SemanticAnalysis parseSemanticResponse(String llmResponse, Intent fallbackIntent) {
        try {
            // Extract JSON from response (may have markdown code blocks)
            String json = extractJson(llmResponse);
            
            JsonNode root = objectMapper.readTree(json);
            
            // Parse intent
            String intentStr = root.path("intent").asText("UNKNOWN");
            Intent intent = parseIntent(intentStr, fallbackIntent);
            
            // Parse confidence
            double confidence = root.path("confidence").asDouble(0.5);
            
            // Parse entities
            Map<String, Object> entities = new HashMap<>();
            JsonNode entitiesNode = root.path("entities");
            if (entitiesNode.isObject()) {
                if (entitiesNode.has("age")) {
                    entities.put("age", entitiesNode.path("age").asInt());
                }
                if (entitiesNode.has("gender")) {
                    entities.put("gender", entitiesNode.path("gender").asText());
                }
                if (entitiesNode.has("price")) {
                    entities.put("price", entitiesNode.path("price").asLong());
                }
                if (entitiesNode.has("minPrice")) {
                    entities.put("minPrice", entitiesNode.path("minPrice").asLong());
                }
                if (entitiesNode.has("maxPrice")) {
                    entities.put("maxPrice", entitiesNode.path("maxPrice").asLong());
                }
                if (entitiesNode.has("quantity")) {
                    entities.put("quantity", entitiesNode.path("quantity").asInt());
                }
            }
            
            // Parse keywords
            List<String> keywords = new ArrayList<>();
            JsonNode keywordsNode = root.path("keywords");
            if (keywordsNode.isArray()) {
                keywordsNode.forEach(node -> keywords.add(node.asText()));
            }
            
            // Parse reasoning
            String reasoning = root.path("reasoning").asText("LLM semantic analysis");
            
            return new SemanticAnalysis(intent, confidence, entities, keywords, reasoning);
            
        } catch (Exception e) {
            logger.error("Failed to parse LLM response: {}", e.getMessage());
            return createFallbackAnalysis(fallbackIntent, 0.5);
        }
    }
    
    /**
     * Extract JSON from LLM response (handle markdown code blocks)
     */
    private String extractJson(String response) {
        // Remove markdown code blocks if present
        String cleaned = response.trim();
        
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }
    
    /**
     * Parse intent string to Intent enum
     */
    private Intent parseIntent(String intentStr, Intent fallback) {
        try {
            return Intent.valueOf(intentStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown intent '{}', using fallback: {}", intentStr, fallback);
            return fallback;
        }
    }
    
    /**
     * Create fallback analysis when LLM fails
     */
    private SemanticAnalysis createFallbackAnalysis(Intent intent, double confidence) {
        return new SemanticAnalysis(
            intent,
            confidence,
            new HashMap<>(),
            new ArrayList<>(),
            "Fallback analysis (LLM unavailable)"
        );
    }
    
    /**
     * Check if LLM assistance is needed
     * 
     * Criteria:
     * - Low confidence (<0.75)
     * - Intent is UNKNOWN
     * - No entities extracted but message seems complex
     */
    public boolean shouldUseLLM(Intent intent, double confidence, 
                                Map<String, Object> entities, String message) {
        // Case 1: Low confidence
        if (confidence < 0.75) {
            logger.info("🔍 LLM assistance needed: Low confidence ({:.2f})", confidence);
            return true;
        }
        
        // Case 2: Unknown intent
        if (intent == Intent.UNKNOWN) {
            logger.info("🔍 LLM assistance needed: Unknown intent");
            return true;
        }
        
        // Case 3: Complex message but no entities
        if (entities.isEmpty() && message.split("\\s+").length > 5) {
            logger.info("🔍 LLM assistance needed: Complex message, no entities");
            return true;
        }
        
        // Case 4: Gift consultation with missing info
        if (intent == Intent.GIFT_CONSULTATION && 
            !entities.containsKey("age") && !entities.containsKey("gender")) {
            logger.info("🔍 LLM assistance needed: Gift consultation missing entities");
            return true;
        }
        
        logger.info("✅ NLU confident, no LLM assistance needed");
        return false;
    }
    
    /**
     * Merge LLM analysis with NLU result
     * LLM suggestions override NLU if confidence is higher
     */
    public static class MergedAnalysis {
        public final Intent intent;
        public final double confidence;
        public final Map<String, Object> entities;
        public final List<String> keywords;
        public final String source; // "NLU", "LLM", or "MERGED"
        
        public MergedAnalysis(Intent intent, double confidence, 
                            Map<String, Object> entities, List<String> keywords, String source) {
            this.intent = intent;
            this.confidence = confidence;
            this.entities = entities;
            this.keywords = keywords;
            this.source = source;
        }
        
        @Override
        public String toString() {
            return String.format("MergedAnalysis[intent=%s, conf=%.2f, source=%s]",
                    intent, confidence, source);
        }
    }
    
    /**
     * Merge NLU and LLM results intelligently
     */
    public MergedAnalysis mergeAnalysis(
            Intent nluIntent, double nluConfidence, 
            Map<String, Object> nluEntities, List<String> nluKeywords,
            SemanticAnalysis llmAnalysis) {
        
        logger.info("🔀 Merging NLU and LLM analysis...");
        
        // If LLM confidence is significantly higher, use LLM
        if (llmAnalysis.getConfidence() > nluConfidence + 0.1) {
            logger.info("   → Using LLM (higher confidence: {:.2f} vs {:.2f})",
                       llmAnalysis.getConfidence(), nluConfidence);
            
            // Merge entities (combine both)
            Map<String, Object> mergedEntities = new HashMap<>(nluEntities);
            mergedEntities.putAll(llmAnalysis.getExtractedEntities());
            
            // Merge keywords (combine both, remove duplicates)
            List<String> mergedKeywords = new ArrayList<>(nluKeywords);
            llmAnalysis.getSuggestedKeywords().forEach(kw -> {
                if (!mergedKeywords.contains(kw)) {
                    mergedKeywords.add(kw);
                }
            });
            
            return new MergedAnalysis(
                llmAnalysis.getSuggestedIntent(),
                llmAnalysis.getConfidence(),
                mergedEntities,
                mergedKeywords,
                "LLM"
            );
        }
        
        // Otherwise, use NLU but enrich with LLM entities
        logger.info("   → Using NLU, enriched with LLM entities");
        
        Map<String, Object> mergedEntities = new HashMap<>(nluEntities);
        // Add LLM entities that NLU missed
        llmAnalysis.getExtractedEntities().forEach((key, value) -> {
            if (!mergedEntities.containsKey(key)) {
                mergedEntities.put(key, value);
            }
        });
        
        // Merge keywords
        List<String> mergedKeywords = new ArrayList<>(nluKeywords);
        llmAnalysis.getSuggestedKeywords().forEach(kw -> {
            if (!mergedKeywords.contains(kw)) {
                mergedKeywords.add(kw);
            }
        });
        
        return new MergedAnalysis(
            nluIntent,
            Math.max(nluConfidence, llmAnalysis.getConfidence()),
            mergedEntities,
            mergedKeywords,
            "MERGED"
        );
    }
}
