package t4m.toy_store.chatbot.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.service.ProductService;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Natural Language Understanding Service
 * Phân tích ngữ nghĩa, trích xuất entities, detect intent với confidence score
 */
@Service
@RequiredArgsConstructor
public class NLUService {
    private static final Logger logger = LoggerFactory.getLogger(NLUService.class);
    
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final LLMSemanticAnalyzer llmAnalyzer; // LLM-assisted semantic analysis
    
    // ==================== INTENT CLASSIFICATION ====================
    
    /**
     * Intent types với confidence threshold
     */
    public enum Intent {
        GREETING(0.9),           // "xin chào", "hi", "hello"
        PRICE_QUERY(0.85),       // "giá bao nhiêu", "giá sản phẩm"
        PRODUCT_SEARCH(0.8),     // "tìm búp bê", "có xe nào"
        POLICY_QUERY(0.85),      // "chính sách", "đổi trả", "giao hàng"
        GIFT_CONSULTATION(0.75), // "tư vấn quà", "mua quà cho bé"
        COMPARISON(0.8),         // "so sánh", "khác nhau"
        RECOMMENDATION(0.75),    // "gợi ý", "đề xuất"
        UNKNOWN(0.0);            // Others → forward to AI
        
        private final double defaultConfidence;
        
        Intent(double defaultConfidence) {
            this.defaultConfidence = defaultConfidence;
        }
        
        public double getDefaultConfidence() {
            return defaultConfidence;
        }
    }
    
    /**
     * Language detection
     */
    public enum Language {
        VIETNAMESE,
        ENGLISH,
        MIXED,
        UNKNOWN
    }
    
    /**
     * NLU Analysis Result
     */
    public static class NLUResult {
        private final Intent intent;
        private final double confidence;
        private final Language language;
        private final List<String> extractedKeywords;
        private final Map<String, Object> entities;
        private final boolean useRuleBased;
        
        public NLUResult(Intent intent, double confidence, Language language, 
                        List<String> keywords, Map<String, Object> entities) {
            this.intent = intent;
            this.confidence = confidence;
            this.language = language;
            this.extractedKeywords = keywords;
            this.entities = entities;
            // Use rule-based if confidence > 0.75 and intent is not UNKNOWN
            this.useRuleBased = confidence >= 0.75 && intent != Intent.UNKNOWN;
        }
        
        public Intent getIntent() { return intent; }
        public double getConfidence() { return confidence; }
        public Language getLanguage() { return language; }
        public List<String> getExtractedKeywords() { return extractedKeywords; }
        public Map<String, Object> getEntities() { return entities; }
        public boolean shouldUseRuleBased() { return useRuleBased; }
        
        @Override
        public String toString() {
            return String.format("NLU[intent=%s, conf=%.2f, lang=%s, keywords=%s, ruleBased=%s]",
                    intent, confidence, language, extractedKeywords, useRuleBased);
        }
    }
    
    // ==================== MAIN NLU ANALYSIS ====================
    
    /**
     * Phân tích toàn diện message của user
     * WITH LLM-ASSISTED SEMANTIC ANALYSIS
     */
    public NLUResult analyze(String message) {
        logger.info("🧠 NLU analyzing: {}", message);
        
        // Step 1: Detect language
        Language language = detectLanguage(message);
        logger.info("Detected language: {}", language);
        
        // Step 2: Normalize message
        String normalized = normalizeMessage(message, language);
        logger.info("Normalized: {}", normalized);
        
        // Step 3: Extract keywords (product-related)
        List<String> keywords = extractProductKeywords(normalized, language);
        logger.info("Extracted keywords: {}", keywords);
        
        // Step 4: Extract entities (price, age, category, etc.)
        Map<String, Object> entities = extractEntities(normalized, language);
        logger.info("Extracted entities: {}", entities);
        
        // Step 5: Classify intent with confidence
        IntentScore intentScore = classifyIntent(normalized, language, keywords, entities);
        logger.info("Intent classification: {} (confidence: {:.2f})", 
                    intentScore.intent, intentScore.confidence);
        
        // 🚀 Step 6: LLM-ASSISTED ANALYSIS (if needed)
        if (llmAnalyzer.shouldUseLLM(intentScore.intent, intentScore.confidence, entities, message)) {
            logger.info("🤖 Invoking LLM for semantic analysis...");
            
            // Call LLM for deep semantic analysis
            LLMSemanticAnalyzer.SemanticAnalysis llmAnalysis = 
                llmAnalyzer.analyzeWithLLM(message, language, intentScore.intent, intentScore.confidence);
            
            logger.info("🤖 LLM Analysis: {}", llmAnalysis);
            logger.info("   Reasoning: {}", llmAnalysis.getReasoning());
            
            // Merge NLU and LLM results
            LLMSemanticAnalyzer.MergedAnalysis merged = 
                llmAnalyzer.mergeAnalysis(
                    intentScore.intent, intentScore.confidence,
                    entities, keywords,
                    llmAnalysis
                );
            
            logger.info("🔀 Merged Analysis: {}", merged);
            
            // Return enhanced NLU result
            NLUResult result = new NLUResult(
                merged.intent,
                merged.confidence,
                language,
                merged.keywords,
                merged.entities
            );
            
            logger.info("✅ NLU Result (LLM-enhanced): {}", result);
            return result;
        }
        
        // Step 7: Return standard NLU result (no LLM needed)
        NLUResult result = new NLUResult(
            intentScore.intent,
            intentScore.confidence,
            language,
            keywords,
            entities
        );
        
        logger.info("✅ NLU Result: {}", result);
        return result;
    }
    
    // ==================== LANGUAGE DETECTION ====================
    
    /**
     * Detect language: Vietnamese, English, Mixed, Unknown
     */
    private Language detectLanguage(String message) {
        String msg = message.toLowerCase();
        
        // Vietnamese indicators
        String[] viIndicators = {
            "à", "á", "ả", "ã", "ạ", "ă", "ằ", "ắ", "ẳ", "ẵ", "ặ",
            "â", "ầ", "ấ", "ẩ", "ẫ", "ậ", "đ", "è", "é", "ẻ", "ẽ", "ẹ",
            "ê", "ề", "ế", "ể", "ễ", "ệ", "ì", "í", "ỉ", "ĩ", "ị",
            "ò", "ó", "ỏ", "õ", "ọ", "ô", "ồ", "ố", "ổ", "ỗ", "ộ",
            "ơ", "ờ", "ớ", "ở", "ỡ", "ợ", "ù", "ú", "ủ", "ũ", "ụ",
            "ư", "ừ", "ứ", "ử", "ữ", "ự", "ỳ", "ý", "ỷ", "ỹ", "ỵ"
        };
        
        // Vietnamese common words
        String[] viWords = {
            "xin chào", "chào", "cảm ơn", "bạn", "tôi", "mình",
            "giá", "bao nhiêu", "tìm", "có", "không", "được",
            "sản phẩm", "đồ chơi", "búp bê", "xe", "lego", "cho",
            "bé", "trẻ em", "tuổi", "năm", "tháng", "quà", "tặng"
        };
        
        // English common words
        String[] enWords = {
            "hello", "hi", "thanks", "price", "how much", "find",
            "search", "product", "toy", "doll", "car", "lego",
            "for", "kid", "child", "year", "old", "gift", "buy"
        };
        
        int viCount = 0;
        int enCount = 0;
        
        // Check Vietnamese diacritics
        for (String indicator : viIndicators) {
            if (msg.contains(indicator)) {
                viCount += 3; // Higher weight for diacritics
                break;
            }
        }
        
        // Check Vietnamese words
        for (String word : viWords) {
            if (msg.contains(word)) viCount++;
        }
        
        // Check English words
        for (String word : enWords) {
            if (msg.contains(word)) enCount++;
        }
        
        if (viCount > 0 && enCount > 0) return Language.MIXED;
        if (viCount > enCount) return Language.VIETNAMESE;
        if (enCount > viCount) return Language.ENGLISH;
        return Language.UNKNOWN;
    }
    
    // ==================== MESSAGE NORMALIZATION ====================
    
    /**
     * Normalize message: lowercase, remove noise, standardize
     */
    private String normalizeMessage(String message, Language language) {
        String normalized = message.toLowerCase().trim();
        
        // Remove extra spaces
        normalized = normalized.replaceAll("\\s+", " ");
        
        // Remove special characters but keep Vietnamese diacritics
        normalized = normalized.replaceAll("[^a-zA-ZÀ-ỹ0-9\\s.,!?-]", " ");
        
        // Remove noise words (stopwords)
        if (language == Language.VIETNAMESE) {
            String[] stopwords = {"ạ", "à", "ơi", "nhé", "nha", "vậy", "thế", "ừ"};
            for (String stop : stopwords) {
                normalized = normalized.replaceAll("\\b" + stop + "\\b", "");
            }
        } else if (language == Language.ENGLISH) {
            String[] stopwords = {"um", "uh", "well", "like", "you know"};
            for (String stop : stopwords) {
                normalized = normalized.replaceAll("\\b" + stop + "\\b", "");
            }
        }
        
        // Trim again after removals
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized;
    }
    
    // ==================== KEYWORD EXTRACTION ====================
    
    /**
     * Extract product-related keywords from message
     */
    private List<String> extractProductKeywords(String message, Language language) {
        List<String> keywords = new ArrayList<>();
        
        // Category keywords (VI + EN)
        Map<String, String[]> categoryKeywords = new HashMap<>();
        categoryKeywords.put("búp bê", new String[]{"búp bê", "doll", "barbie", "elsa", "anna"});
        categoryKeywords.put("xe ô tô", new String[]{"xe", "ô tô", "car", "vehicle", "xe hơi", "xe tải"});
        categoryKeywords.put("lego", new String[]{"lego", "xếp hình", "building", "block"});
        categoryKeywords.put("robot", new String[]{"robot", "rô bốt", "transformer", "biến hình"});
        categoryKeywords.put("thú bông", new String[]{"thú bông", "stuffed", "plush", "gấu", "bear"});
        categoryKeywords.put("đồ chơi giáo dục", new String[]{"giáo dục", "educational", "học", "learning"});
        categoryKeywords.put("đồ chơi thể thao", new String[]{"thể thao", "sport", "bóng", "ball"});
        categoryKeywords.put("nhà búp bê", new String[]{"nhà búp bê", "dollhouse", "playhouse", "nhà chơi"});
        
        // Check category matches
        for (Map.Entry<String, String[]> entry : categoryKeywords.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (message.contains(keyword.toLowerCase())) {
                    keywords.add(entry.getKey());
                    break;
                }
            }
        }
        
        // Extract brand names
        String[] brands = {"lego", "barbie", "disney", "marvel", "hasbro"};
        for (String brand : brands) {
            if (message.contains(brand)) {
                keywords.add(brand);
            }
        }
        
        // Extract specific product names by checking against database
        String[] words = message.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            // Try 1-word, 2-word, 3-word combinations
            for (int len = 1; len <= Math.min(3, words.length - i); len++) {
                String phrase = String.join(" ", Arrays.copyOfRange(words, i, i + len));
                if (phrase.length() >= 3) { // Min 3 characters
                    // Check if this phrase matches any product name
                    Page<Product> matchedProducts = productService.searchProducts(phrase, PageRequest.of(0, 1));
                    if (matchedProducts.hasContent()) {
                        keywords.add(phrase);
                    }
                }
            }
        }
        
        // Remove duplicates
        return keywords.stream().distinct().collect(Collectors.toList());
    }
    
    // ==================== ENTITY EXTRACTION ====================
    
    /**
     * Extract entities: price, age, quantity, category, etc.
     */
    private Map<String, Object> extractEntities(String message, Language language) {
        Map<String, Object> entities = new HashMap<>();
        
        // Extract PRICE (giá, price, VND, đ)
        Pattern pricePattern = Pattern.compile("(\\d+[.,]?\\d*)\\s*(k|triệu|tr|nghìn|ng|đồng|đ|vnd|usd|\\$)?");
        Matcher priceMatcher = pricePattern.matcher(message);
        if (priceMatcher.find()) {
            String priceStr = priceMatcher.group(1).replace(",", ".");
            String unit = priceMatcher.group(2);
            try {
                double price = Double.parseDouble(priceStr);
                // Convert to VND
                if (unit != null) {
                    if (unit.equals("k") || unit.equals("nghìn") || unit.equals("ng")) {
                        price *= 1000;
                    } else if (unit.equals("triệu") || unit.equals("tr")) {
                        price *= 1000000;
                    }
                }
                entities.put("price", BigDecimal.valueOf(price));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse price: {}", priceStr);
            }
        }
        
        // Extract AGE (tuổi, year, old)
        Pattern agePattern = Pattern.compile("(\\d+)\\s*(tuổi|năm|year|yo|old)");
        Matcher ageMatcher = agePattern.matcher(message);
        if (ageMatcher.find()) {
            try {
                int age = Integer.parseInt(ageMatcher.group(1));
                entities.put("age", age);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse age: {}", ageMatcher.group(1));
            }
        }
        
        // Extract QUANTITY (số lượng, quantity)
        Pattern qtyPattern = Pattern.compile("(\\d+)\\s*(cái|sản phẩm|món|items?|pieces?)");
        Matcher qtyMatcher = qtyPattern.matcher(message);
        if (qtyMatcher.find()) {
            try {
                int qty = Integer.parseInt(qtyMatcher.group(1));
                entities.put("quantity", qty);
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse quantity: {}", qtyMatcher.group(1));
            }
        }
        
        // Extract GENDER (giới tính, gender)
        if (message.matches(".*(bé trai|con trai|boy|male).*")) {
            entities.put("gender", "male");
        } else if (message.matches(".*(bé gái|con gái|girl|female).*")) {
            entities.put("gender", "female");
        }
        
        // Extract PRICE RANGE (khoảng giá, budget)
        Pattern rangePattern = Pattern.compile("(\\d+[.,]?\\d*)\\s*[-đến|to|~]\\s*(\\d+[.,]?\\d*)");
        Matcher rangeMatcher = rangePattern.matcher(message);
        if (rangeMatcher.find()) {
            try {
                double minPrice = Double.parseDouble(rangeMatcher.group(1).replace(",", "."));
                double maxPrice = Double.parseDouble(rangeMatcher.group(2).replace(",", "."));
                entities.put("minPrice", BigDecimal.valueOf(minPrice));
                entities.put("maxPrice", BigDecimal.valueOf(maxPrice));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse price range");
            }
        }
        
        return entities;
    }
    
    // ==================== INTENT CLASSIFICATION ====================
    
    /**
     * Intent with confidence score
     */
    private static class IntentScore {
        Intent intent;
        double confidence;
        
        IntentScore(Intent intent, double confidence) {
            this.intent = intent;
            this.confidence = confidence;
        }
    }
    
    /**
     * Classify intent with confidence score
     */
    private IntentScore classifyIntent(String message, Language language, 
                                       List<String> keywords, Map<String, Object> entities) {
        
        // Multiple intent patterns with weights
        Map<Intent, Double> scores = new HashMap<>();
        scores.put(Intent.GREETING, 0.0);
        scores.put(Intent.PRICE_QUERY, 0.0);
        scores.put(Intent.PRODUCT_SEARCH, 0.0);
        scores.put(Intent.POLICY_QUERY, 0.0);
        scores.put(Intent.GIFT_CONSULTATION, 0.0);
        scores.put(Intent.COMPARISON, 0.0);
        scores.put(Intent.RECOMMENDATION, 0.0);
        
        // GREETING patterns
        if (message.matches("^(xin chào|chào|hi|hello|hey|chao).*")) {
            scores.put(Intent.GREETING, scores.get(Intent.GREETING) + 0.9);
        }
        
        // PRICE QUERY patterns
        if (message.matches(".*(giá|price|cost|giá tiền|giá cả|bao nhiêu|how much).*")) {
            scores.put(Intent.PRICE_QUERY, scores.get(Intent.PRICE_QUERY) + 0.5);
        }
        if (!keywords.isEmpty() && entities.containsKey("price")) {
            scores.put(Intent.PRICE_QUERY, scores.get(Intent.PRICE_QUERY) + 0.3);
        }
        if (message.matches(".*(giá của|price of|giá sản phẩm).*")) {
            scores.put(Intent.PRICE_QUERY, scores.get(Intent.PRICE_QUERY) + 0.2);
        }
        
        // PRODUCT SEARCH patterns
        if (message.matches(".*(tìm|search|find|có|show|xem|hiển thị).*") && !keywords.isEmpty()) {
            scores.put(Intent.PRODUCT_SEARCH, scores.get(Intent.PRODUCT_SEARCH) + 0.6);
        }
        if (message.matches(".*(sản phẩm|product|đồ chơi|toy).*")) {
            scores.put(Intent.PRODUCT_SEARCH, scores.get(Intent.PRODUCT_SEARCH) + 0.2);
        }
        if (keywords.size() >= 2) {
            scores.put(Intent.PRODUCT_SEARCH, scores.get(Intent.PRODUCT_SEARCH) + 0.2);
        }
        
        // POLICY QUERY patterns
        if (message.matches(".*(chính sách|policy|đổi trả|return|hoàn tiền|refund|giao hàng|shipping|bảo hành|warranty).*")) {
            scores.put(Intent.POLICY_QUERY, scores.get(Intent.POLICY_QUERY) + 0.8);
        }
        
        // GIFT CONSULTATION patterns
        if (message.matches(".*(tư vấn|consult|gợi ý|suggest|mua quà|gift|tặng|present).*")) {
            scores.put(Intent.GIFT_CONSULTATION, scores.get(Intent.GIFT_CONSULTATION) + 0.5);
        }
        if (entities.containsKey("age") || entities.containsKey("gender")) {
            scores.put(Intent.GIFT_CONSULTATION, scores.get(Intent.GIFT_CONSULTATION) + 0.3);
        }
        if (message.matches(".*(cho bé|for kid|trẻ em|child).*")) {
            scores.put(Intent.GIFT_CONSULTATION, scores.get(Intent.GIFT_CONSULTATION) + 0.2);
        }
        
        // COMPARISON patterns
        if (message.matches(".*(so sánh|compare|khác nhau|difference|tốt hơn|better).*")) {
            scores.put(Intent.COMPARISON, scores.get(Intent.COMPARISON) + 0.7);
        }
        if (keywords.size() >= 2) {
            scores.put(Intent.COMPARISON, scores.get(Intent.COMPARISON) + 0.2);
        }
        
        // RECOMMENDATION patterns
        if (message.matches(".*(đề xuất|recommend|gợi ý|suggest|nên|should).*")) {
            scores.put(Intent.RECOMMENDATION, scores.get(Intent.RECOMMENDATION) + 0.6);
        }
        
        // Find highest score
        Intent bestIntent = Intent.UNKNOWN;
        double maxScore = 0.0;
        
        for (Map.Entry<Intent, Double> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                bestIntent = entry.getKey();
            }
        }
        
        // Normalize confidence to 0-1 range
        double confidence = Math.min(1.0, maxScore);
        
        // If no clear intent, return UNKNOWN
        if (confidence < 0.5) {
            return new IntentScore(Intent.UNKNOWN, confidence);
        }
        
        return new IntentScore(bestIntent, confidence);
    }
}
