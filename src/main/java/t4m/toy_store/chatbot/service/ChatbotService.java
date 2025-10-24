package t4m.toy_store.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.service.ProductService;
import t4m.toy_store.chatbot.service.NLUService;
import t4m.toy_store.chatbot.service.NLUService.Intent;
import t4m.toy_store.chatbot.service.NLUService.NLUResult;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final NLUService nluService; // NLU Service for intelligent intent detection
    
    // Store conversation history (conversationId -> list of messages)
    // In production, use Redis or database for scalability
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();
    
    // Base system prompt for children's toy store (CONTEXT-AWARE, NON-SEQUENTIAL)
    private static final String BASE_SYSTEM_PROMPT = 
        "Bạn là AI tư vấn đồ chơi T4M cho trẻ em. Phong cách: thân thiện, vui vẻ, ngắn gọn.\n\n" +
        
        "NGUYÊN TẮC HOẠT ĐỘNG:\n" +
        "1. PHÂN TÍCH NGỮ CẢNH: Đọc kỹ câu hỏi của user, xác định họ cần gì\n" +
        "2. TRẢ LỜI TRỰC TIẾP: Nếu đủ thông tin trong câu hỏi → Trả lời ngay, KHÔNG hỏi lại\n" +
        "3. CHỈ HỎI KHI THIẾU: Chỉ hỏi thêm khi thực sự thiếu thông tin quan trọng\n\n" +
        
        "CÁC TÌNH HUỐNG:\n\n" +
        
        "A. HỎI VỀ SẢN PHẨM CỤ THỂ (Giá, Tìm kiếm, Thông tin):\n" +
        "   → TRẢ LỜI NGAY với thông tin từ DANH SÁCH SẢN PHẨM\n" +
        "   - Nếu CÓ sản phẩm: Liệt kê 3-5 sản phẩm phù hợp nhất\n" +
        "   - Nếu KHÔNG CÓ: 'Xin lỗi, hiện tại T4M chưa có sản phẩm này. Tôi gợi ý những sản phẩm HOT:' → Gợi ý 3-4 sản phẩm hot từ danh sách\n" +
        "   VD: 'Giá xe tăng?' → Trả lời giá các xe tăng có trong danh sách\n" +
        "   VD: 'Có búp bê Elsa không?' → Liệt kê Elsa nếu có, nếu không thì gợi ý búp bê khác\n\n" +
        
        "B. TƯ VẤN QUÀ CÓ ĐỦ THÔNG TIN (tuổi/giới tính/sở thích rõ ràng):\n" +
        "   → TRẢ LỜI NGAY với gợi ý phù hợp\n" +
        "   - Phân tích: tuổi, giới tính, sở thích từ câu hỏi\n" +
        "   - Gợi ý 3-4 sản phẩm CÓ TRONG DANH SÁCH phù hợp nhất\n" +
        "   - Nếu KHÔNG có sản phẩm phù hợp: 'Xin lỗi, T4M chưa có sản phẩm này. Gợi ý sản phẩm HOT:' → Liệt kê 3-4 sản phẩm hot\n" +
        "   VD: 'Quà cho bé gái 5 tuổi thích công chúa' → Gợi ý búp bê công chúa ngay\n" +
        "   VD: 'Đồ chơi cho bé trai 8 tuổi thích robot' → Gợi ý robot/transformer ngay\n\n" +
        
        "C. TƯ VẤN QUÀ THIẾU THÔNG TIN QUAN TRỌNG:\n" +
        "   → CHỈ HỎI những thông tin thiếu, KHÔNG hỏi lại thông tin đã có\n" +
        "   - Nếu thiếu tuổi: 'Bé bao nhiêu tuổi ạ?'\n" +
        "   - Nếu thiếu giới tính: 'Bé là con trai hay con gái ạ?'\n" +
        "   - Nếu thiếu sở thích: 'Bé thích loại đồ chơi nào ạ? (VD: búp bê, xe, lego, khoa học...)'\n" +
        "   VD: 'Tư vấn quà cho bé' → Hỏi: 'Bé bao nhiêu tuổi và là con trai hay con gái ạ?'\n\n" +
        
        "D. CHÍNH SÁCH/HỖ TRỢ:\n" +
        "   → TRẢ LỜI NGAY thông tin chính sách\n" +
        "   - Đổi trả: 7 ngày, sản phẩm nguyên tem\n" +
        "   - Giao hàng: 1-3 ngày, miễn phí từ 300K\n" +
        "   - Hotline: 1800-363-363\n\n" +
        
        "E. KHÁCH CHỌN SẢN PHẨM (nói tên hoặc 'tôi chọn...'):\n" +
        "   → BẮT BUỘC trả lời:\n" +
        "   'Cảm ơn bạn đã chọn <TÊN SẢN PHẨM>! 🎁\n\n" +
        "   Bạn hãy tìm kiếm \"<TÊN SẢN PHẨM>\" trên web T4M để xem chi tiết và đặt hàng.\n\n" +
        "   Chúc bạn mua sắm vui vẻ! 😊'\n\n" +
        
        "⚠️ FORMAT SẢN PHẨM (BẮT BUỘC):\n" +
        "   - MỖI SẢN PHẨM MỘT DÒNG (xuống dòng sau mỗi sản phẩm)\n" +
        "   - Format: • Tên | Trạng thái | Mô tả ngắn\n" +
        "   - KHÔNG ghi giá, KHÔNG gộp nhiều sản phẩm 1 dòng\n" +
        "   VD ĐÚNG:\n" +
        "   • Búp bê Elsa | Còn hàng, SALE | Công chúa băng giá xinh đẹp\n" +
        "   • Búp bê Barbie | Còn hàng | Ngôi nhà mơ ước\n" +
        "   • Búp bê Jasmine | Hết hàng | Công chúa Jasmine quyến rũ\n\n" +
        
        "LƯU Ý:\n" +
        "- LUÔN ưu tiên trả lời trực tiếp nếu có đủ thông tin\n" +
        "- CHỈ gợi ý sản phẩm CÓ TRONG DANH SÁCH\n" +
        "- Nếu KHÔNG có sản phẩm phù hợp → Xin lỗi + gợi ý sản phẩm HOT\n" +
        "- Trả lời NGẮN GỌN, ÍT EMOJI\n" +
        "- KHÔNG hỏi lại thông tin user đã cung cấp\n\n";

    
    public String generateResponse(String userMessage, String conversationId) {
        logger.info("=== ChatbotService.generateResponse CALLED (HYBRID MODE) ===");
        logger.info("User message: {}, Conversation ID: {}", userMessage, conversationId);
        logger.info("Gemini API key configured: {}", geminiApiKey != null && !geminiApiKey.isEmpty());
        
        if (geminiApiKey == null || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            logger.warn("Gemini API key not configured");
            return "Xin lỗi bạn, chatbot AI chưa được cấu hình. Vui lòng liên hệ quản trị viên hoặc gọi hotline 1800-363-363 để được hỗ trợ! 😊";
        }
        
        try {
            // 🧠 NLU ANALYSIS: Detect intent, extract entities, analyze semantics
            NLUResult nluResult = nluService.analyze(userMessage);
            logger.info("🧠 NLU Result: {}", nluResult);
            logger.info("   → Intent: {} (confidence: {:.2f}%)", 
                       nluResult.getIntent(), nluResult.getConfidence() * 100);
            logger.info("   → Language: {}", nluResult.getLanguage());
            logger.info("   → Keywords: {}", nluResult.getExtractedKeywords());
            logger.info("   → Entities: {}", nluResult.getEntities());
            logger.info("   → Use Rule-Based: {}", nluResult.shouldUseRuleBased());
            
            // STEP 2: Handle with rule-based if NLU recommends (FAST PATH ⚡)
            if (nluResult.shouldUseRuleBased()) {
                Intent intent = nluResult.getIntent();
                
                switch (intent) {
                    case GREETING:
                        logger.info("⚡ Using rule-based handler for GREETING");
                        return handleGreeting();
                        
                    case PRICE_QUERY:
                        logger.info("⚡ Using rule-based handler for PRICE_QUERY");
                        return handlePriceQueryAdvanced(userMessage, nluResult);
                        
                    case PRODUCT_SEARCH:
                        logger.info("⚡ Using rule-based handler for PRODUCT_SEARCH");
                        return handleProductSearchAdvanced(userMessage, nluResult);
                        
                    case POLICY_QUERY:
                        logger.info("⚡ Using rule-based handler for POLICY_QUERY");
                        return handlePolicyQuery(userMessage);
                        
                    case COMPARISON:
                        logger.info("⚡ Using rule-based handler for COMPARISON");
                        return handleComparison(nluResult);
                        
                    case RECOMMENDATION:
                        logger.info("⚡ Using rule-based handler for RECOMMENDATION");
                        return handleRecommendation(nluResult);
                        
                    case GIFT_CONSULTATION:
                    case UNKNOWN:
                        // Fall through to AI (SMART PATH 🤖)
                        logger.info("🤖 Low confidence or complex query - forwarding to AI");
                        break;
                }
            } else {
                logger.info("🤖 NLU recommends AI path (confidence too low or complex query)");
            }
            
            // STEP 3: Handle with AI for complex queries (ORIGINAL LOGIC)
            // Get or create conversation history
            List<Map<String, String>> history = conversationHistory.computeIfAbsent(conversationId, k -> new ArrayList<>());
            
            // 🔍 ENRICH WITH DATABASE CONTEXT: Always check if products exist
            String productContext = "";
            List<Product> matchedProducts = new ArrayList<>();
            
            // Try to find products based on NLU keywords
            if (!nluResult.getExtractedKeywords().isEmpty()) {
                logger.info("🔍 Searching products with NLU keywords: {}", nluResult.getExtractedKeywords());
                for (String keyword : nluResult.getExtractedKeywords()) {
                    List<Product> found = productService.searchProducts(keyword, PageRequest.of(0, 10)).getContent();
                    if (!found.isEmpty()) {
                        matchedProducts.addAll(found);
                        logger.info("   ✅ Found {} products for keyword '{}'", found.size(), keyword);
                    }
                }
            }
            
            // Build context based on whether products exist
            if (!matchedProducts.isEmpty()) {
                // Products found: provide details to AI
                logger.info("✅ Found {} relevant products - enriching AI with product details", matchedProducts.size());
                StringBuilder sb = new StringBuilder("\n\nSẢN PHẨM LIÊN QUAN:\n");
                matchedProducts.stream()
                    .distinct()
                    .limit(10)
                    .forEach(p -> {
                        sb.append(String.format("- %s | Giá: %s | Còn: %d | %s\n",
                            p.getName(),
                            formatCurrency(p.getPrice()),
                            p.getStock(),
                            p.getStock() > 0 ? "✅ Còn hàng" : "❌ Hết hàng"));
                    });
                productContext = sb.toString();
            } else {
                // No products: tell AI explicitly
                logger.info("❌ No products found for keywords: {}", nluResult.getExtractedKeywords());
                productContext = "\n\n⚠️ KHÔNG TÌM THẤY SẢN PHẨM PHÙ HỢP với từ khóa: " + 
                               String.join(", ", nluResult.getExtractedKeywords()) + 
                               "\n→ Hãy xin lỗi và GỢI Ý 3-4 SẢN PHẨM HOT khác (robot, lego, búp bê, xe điều khiển)\n";
            }
            
            // SMART CONTEXT LOADING: Load general categories if needed (for first message)
            if (history.isEmpty() && matchedProducts.isEmpty()) {
                // First message: only show category overview (lightweight)
                logger.info("First message - loading category overview...");
                productContext = buildProductContext(); // Returns category overview
                logger.info("Category overview built: {} characters", productContext.length());
            } else if (history.size() >= 2) {
                // After user answered preference question, load specific products
                logger.info("User specified preference - loading relevant products...");
                productContext = buildProductsByKeywords(userMessage);
                logger.info("Product context built: {} characters, Message: {}", productContext.length(), userMessage);
            } else {
                logger.info("Second message (gender question) - no product loading needed");
                productContext = ""; // AI already knows categories
            }
            
            // Add user message to history
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("message", userMessage);
            history.add(userMsg);
            
            // Build conversation context from history (keep it SHORT)
            StringBuilder conversationContext = new StringBuilder();
            // Only include recent messages (last 6 messages = 3 turns)
            int startIdx = Math.max(0, history.size() - 6);
            for (int i = startIdx; i < history.size(); i++) {
                Map<String, String> msg = history.get(i);
                if ("user".equals(msg.get("role"))) {
                    conversationContext.append("Khách: ").append(msg.get("message")).append("\n");
                } else if ("assistant".equals(msg.get("role"))) {
                    conversationContext.append("AI: ").append(msg.get("message")).append("\n");
                }
            }
            
            // Build the full prompt
            String fullPrompt;
            if (history.size() == 1) {
                // First message: include category overview
                fullPrompt = BASE_SYSTEM_PROMPT + productContext + 
                           "\n\nKhách hàng hỏi: " + userMessage + "\n\nTrả lời:";
            } else if (!productContext.isEmpty()) {
                // User specified preference: include specific products + conversation
                fullPrompt = BASE_SYSTEM_PROMPT + productContext +
                           "\n\nHỘI THOẠI GẦN ĐÂY:\n" + conversationContext.toString() + 
                           "\n\nDựa vào danh sách sản phẩm ở trên, gợi ý 3-4 sản phẩm TỐT NHẤT cho khách:";
            } else {
                // Other follow-up messages: just conversation history
                fullPrompt = BASE_SYSTEM_PROMPT + 
                           "\n\nHỘI THOẠI GẦN ĐÂY:\n" + conversationContext.toString() + 
                           "\n\nTrả lời tin nhắn mới nhất (ngắn gọn):";
            }
            
            logger.info("Prompt length: {} chars (~{} tokens), History: {} msgs", 
                       fullPrompt.length(), fullPrompt.length() / 4, history.size());
            
            // Gemini API endpoint (using v1 stable API with gemini-2.5-flash - latest model)
            String apiUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", fullPrompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));
            
            // Add generation config for better responses
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 2048); // Increased to 2048 for product recommendations
            requestBody.put("generationConfig", generationConfig);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // Call Gemini API with retry logic
            ResponseEntity<String> response = callGeminiAPIWithRetry(apiUrl, entity);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody();
                logger.debug("Gemini API response body: {}", responseBody);
                
                // Parse response
                JsonNode root = objectMapper.readTree(responseBody);
                
                // Check for error in response
                if (root.has("error")) {
                    JsonNode error = root.get("error");
                    String errorMsg = error.path("message").asText("Unknown error");
                    logger.error("Gemini API error: {}", errorMsg);
                    return "Xin lỗi bạn, mình gặp lỗi từ AI: " + errorMsg + ". Vui lòng thử lại sau nhé! 😊";
                }
                
                // Check if response has candidates
                if (!root.has("candidates")) {
                    logger.error("Response missing 'candidates' field. Full response: {}", responseBody);
                    return "Xin lỗi bạn, AI không trả lời được. Vui lòng thử lại hoặc gọi hotline 1800-363-363 nhé! 😊";
                }
                
                JsonNode candidates = root.get("candidates");
                
                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    
                    // Check if blocked by safety filters
                    if (firstCandidate.has("finishReason")) {
                        String finishReason = firstCandidate.get("finishReason").asText();
                        if ("SAFETY".equals(finishReason)) {
                            logger.warn("Response blocked by safety filters");
                            return "Xin lỗi bạn, câu hỏi này không phù hợp. Bạn có thể hỏi về sản phẩm hoặc gọi hotline 1800-363-363 nhé! 😊";
                        }
                    }
                    
                    JsonNode content_node = firstCandidate.path("content");
                    JsonNode parts = content_node.path("parts");
                    
                    if (parts.isArray() && parts.size() > 0) {
                        String aiResponse = parts.get(0).path("text").asText();
                        
                        if (aiResponse == null || aiResponse.trim().isEmpty()) {
                            logger.error("AI response is empty. Candidate: {}", firstCandidate.toString());
                            return "Xin lỗi bạn, mình không có câu trả lời phù hợp. Vui lòng thử lại hoặc gọi hotline 1800-363-363 nhé! 😊";
                        }
                        
                        logger.info("Gemini AI response generated successfully for conversation: {}", conversationId);
                        
                        // Add AI response to history
                        Map<String, String> assistantMsg = new HashMap<>();
                        assistantMsg.put("role", "assistant");
                        assistantMsg.put("message", aiResponse);
                        conversationHistory.get(conversationId).add(assistantMsg);
                        
                        return aiResponse;
                    } else {
                        logger.error("Parts array is empty or missing. Candidate: {}", firstCandidate.toString());
                    }
                } else {
                    logger.error("Candidates array is empty or missing. Root: {}", root.toString());
                }
            } else {
                logger.error("Gemini API returned status: {}, body: {}", response.getStatusCode(), response.getBody());
            }
            
            logger.warn("Unexpected response format from Gemini API. Could not extract text from response.");
            return "Xin lỗi bạn, mình không thể trả lời câu hỏi này ngay bây giờ. Bạn có thể thử hỏi câu khác hoặc liên hệ hotline 1800-363-363 nhé! 😊";
            
        } catch (Exception e) {
            logger.error("Error calling Gemini API for conversation: " + conversationId, e);
            logger.error("Error details - Type: {}, Message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            
            // Check if it's an overload error
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("overloaded") || errorMsg.contains("503"))) {
                return "Xin lỗi bạn, hệ thống AI đang quá tải. Vui lòng thử lại sau 1-2 phút hoặc gọi hotline 1800-363-363 để được hỗ trợ trực tiếp nhé! 😊";
            }
            
            return "Xin lỗi bạn, mình đang gặp chút trục trặc kỹ thuật. Bạn thử lại sau hoặc gọi hotline 1800-363-363 để được hỗ trợ trực tiếp nhé! 😊";
        }
    }
    
    public String generateConversationId() {
        return UUID.randomUUID().toString();
    }
    
    // ==================== RULE-BASED HANDLERS ====================
    
    /**
     * Handle greeting intent
     */
    private String handleGreeting() {
        return "Chào bạn! Mình là T4M AI Trợ lý 🤖\n\n" +
               "Mình có thể giúp bạn:\n" +
               "• Tư vấn quà cho bé\n" +
               "• Tìm kiếm sản phẩm\n" +
               "• Hỏi về giá cả\n" +
               "• Chính sách đổi trả, giao hàng\n\n" +
               "Bạn cần giúp gì ạ? 😊";
    }
    
    // ==================== ADVANCED NLU-POWERED HANDLERS ====================
    
    /**
     * Advanced price query handler with NLU entities
     */
    private String handlePriceQueryAdvanced(String message, NLUResult nluResult) {
        // Extract keywords from NLU
        List<String> keywords = nluResult.getExtractedKeywords();
        String keyword = keywords.isEmpty() ? extractKeyword(message) : keywords.get(0);
        
        if (keyword.isEmpty()) {
            return "Bạn muốn hỏi giá sản phẩm nào ạ? 💰\n" +
                   "Ví dụ: 'Giá búp bê Elsa', 'Xe ô tô bao nhiêu tiền'";
        }
        
        logger.info("🔍 Searching products with keyword: {}", keyword);
        List<Product> products = productService.searchProducts(keyword, PageRequest.of(0, 5)).getContent();
        
        if (products.isEmpty()) {
            return "Xin lỗi bạn, mình không tìm thấy sản phẩm nào có từ khóa '" + keyword + "' 😢\n\n" +
                   "Bạn có thể thử:\n" +
                   "• Tìm theo danh mục: 'Búp bê', 'Xe', 'Lego', 'Robot'\n" +
                   "• Gọi hotline 1800-363-363 để được tư vấn trực tiếp";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("💰 Giá sản phẩm liên quan đến \"").append(keyword).append("\":\n\n");
        
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        
        for (Product p : products) {
            response.append("• ").append(p.getName()).append("\n");
            
            if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0) {
                response.append("  Giá gốc: ").append(vndFormat.format(p.getPrice())).append("\n");
                response.append("  Giá SALE: ").append(vndFormat.format(p.getDiscountPrice())).append(" 🔥\n");
            } else {
                response.append("  Giá: ").append(vndFormat.format(p.getPrice())).append("\n");
            }
            
            if (p.getStock() != null && p.getStock() > 0) {
                response.append("  ✅ Còn hàng\n");
            } else {
                response.append("  ❌ Hết hàng\n");
            }
            response.append("\n");
        }
        
        response.append("Tìm kiếm \"").append(keyword).append("\" trên website T4M để xem chi tiết! 🎁");
        return response.toString();
    }
    
    /**
     * Advanced product search handler with NLU entities
     */
    private String handleProductSearchAdvanced(String message, NLUResult nluResult) {
        // Extract keywords from NLU
        List<String> keywords = nluResult.getExtractedKeywords();
        String keyword = keywords.isEmpty() ? extractKeyword(message) : keywords.get(0);
        
        if (keyword.isEmpty()) {
            return "Bạn đang tìm sản phẩm gì ạ? 🔍\n" +
                   "Ví dụ: 'Tìm búp bê Elsa', 'Có xe nào đẹp không'";
        }
        
        logger.info("🔍 Searching products with keyword: {}", keyword);
        List<Product> products = productService.searchProducts(keyword, PageRequest.of(0, 8)).getContent();
        
        if (products.isEmpty()) {
            return "Xin lỗi bạn, không tìm thấy sản phẩm nào phù hợp 😢\n\n" +
                   "🏷️ Danh mục có sẵn:\n" +
                   "👸 Búp bê | 🚀 Xe | 🧩 Lego | 🔬 Khoa học\n" +
                   "⚽ Thể thao | 🎨 Nghệ thuật | 🤖 Robot | 🎲 Board Game";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("🔍 Tìm thấy ").append(products.size()).append(" sản phẩm liên quan \"")
               .append(keyword).append("\":\n\n");
        
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        
        for (Product p : products) {
            response.append("• ").append(p.getName());
            
            if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0) {
                response.append(" | ").append(vndFormat.format(p.getDiscountPrice())).append(" 🔥");
            } else {
                response.append(" | ").append(vndFormat.format(p.getPrice()));
            }
            
            if (p.getStock() != null && p.getStock() > 0) {
                response.append(" | ✅");
            } else {
                response.append(" | ❌");
            }
            response.append("\n");
        }
        
        response.append("\nTìm kiếm \"").append(keyword).append("\" trên web T4M để xem chi tiết! 🛒");
        return response.toString();
    }
    
    /**
     * Handle product comparison
     */
    private String handleComparison(NLUResult nluResult) {
        List<String> keywords = nluResult.getExtractedKeywords();
        
        if (keywords.size() < 2) {
            return "Để so sánh, bạn hãy cho mình biết 2 sản phẩm cần so sánh nhé! 🔄\n" +
                   "Ví dụ: 'So sánh Lego Classic với Lego Technic'";
        }
        
        // Get products for comparison
        String keyword1 = keywords.get(0);
        String keyword2 = keywords.get(1);
        
        List<Product> products1 = productService.searchProducts(keyword1, PageRequest.of(0, 1)).getContent();
        List<Product> products2 = productService.searchProducts(keyword2, PageRequest.of(0, 1)).getContent();
        
        if (products1.isEmpty() || products2.isEmpty()) {
            return "Xin lỗi, mình không tìm thấy đủ thông tin để so sánh 😢\n" +
                   "Bạn thử hỏi AI nhé: 'Tư vấn giúp tôi chọn giữa " + keyword1 + " và " + keyword2 + "'";
        }
        
        Product p1 = products1.get(0);
        Product p2 = products2.get(0);
        
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        
        return String.format(
            "🔄 So sánh 2 sản phẩm:\n\n" +
            "📦 %s\n" +
            "   Giá: %s\n" +
            "   Danh mục: %s\n" +
            "   Trạng thái: %s\n\n" +
            "📦 %s\n" +
            "   Giá: %s\n" +
            "   Danh mục: %s\n" +
            "   Trạng thái: %s\n\n" +
            "Bạn muốn biết thêm chi tiết gì về 2 sản phẩm này không? 😊",
            p1.getName(),
            vndFormat.format(p1.getDiscountPrice() != null ? p1.getDiscountPrice() : p1.getPrice()),
            p1.getCategory().getName(),
            (p1.getStock() != null && p1.getStock() > 0) ? "Còn hàng ✅" : "Hết hàng ❌",
            p2.getName(),
            vndFormat.format(p2.getDiscountPrice() != null ? p2.getDiscountPrice() : p2.getPrice()),
            p2.getCategory().getName(),
            (p2.getStock() != null && p2.getStock() > 0) ? "Còn hàng ✅" : "Hết hàng ❌"
        );
    }
    
    /**
     * Handle product recommendation based on NLU entities
     */
    private String handleRecommendation(NLUResult nluResult) {
        Map<String, Object> entities = nluResult.getEntities();
        
        // Check if we have age or gender info
        Integer age = (Integer) entities.get("age");
        String gender = (String) entities.get("gender");
        
        StringBuilder response = new StringBuilder();
        response.append("💡 Gợi ý sản phẩm từ T4M:\n\n");
        
        // Recommend based on age
        String ageRange = "";
        if (age != null) {
            if (age <= 3) ageRange = "0-3 tuổi";
            else if (age <= 6) ageRange = "3-6 tuổi";
            else if (age <= 12) ageRange = "6-12 tuổi";
            else ageRange = "12+ tuổi";
            
            response.append("Độ tuổi: ").append(age).append(" tuổi (").append(ageRange).append(")\n");
        }
        
        if (gender != null) {
            response.append("Giới tính: ").append(gender.equals("male") ? "Bé trai" : "Bé gái").append("\n");
        }
        
        response.append("\nĐể tư vấn chính xác nhất, mình sẽ hỏi thêm vài câu nữa nhé! 😊\n");
        response.append("Bạn có thể hỏi AI: 'Tư vấn quà cho bé");
        if (age != null) response.append(" ").append(age).append(" tuổi");
        if (gender != null) response.append(gender.equals("male") ? " trai" : " gái");
        response.append("'");
        
        return response.toString();
    }
    
    // ==================== LEGACY HANDLERS (for backward compatibility) ====================
    
    /**
     * Handle price query - search products and show prices
     * @deprecated Use handlePriceQueryAdvanced instead
     */
    @Deprecated
    private String handlePriceQuery(String message) {
        String keyword = extractKeyword(message);
        
        if (keyword.isEmpty()) {
            return "Bạn muốn hỏi giá sản phẩm nào ạ? 💰\n" +
                   "Ví dụ: 'Giá búp bê Elsa', 'Xe ô tô bao nhiêu tiền'";
        }
        
        List<Product> products = productService.searchProducts(keyword, PageRequest.of(0, 5)).getContent();
        
        if (products.isEmpty()) {
            return "Xin lỗi bạn, mình không tìm thấy sản phẩm nào có từ khóa '" + keyword + "' 😢\n\n" +
                   "Bạn có thể thử:\n" +
                   "• Tìm theo danh mục: 'Búp bê', 'Xe', 'Lego', 'Robot'\n" +
                   "• Gọi hotline 1800-363-363 để được tư vấn trực tiếp";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("💰 Giá sản phẩm liên quan đến \"").append(keyword).append("\":\n\n");
        
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        
        for (Product p : products) {
            response.append("• ").append(p.getName()).append("\n");
            
            if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0) {
                response.append("  Giá gốc: ").append(vndFormat.format(p.getPrice())).append("\n");
                response.append("  Giá SALE: ").append(vndFormat.format(p.getDiscountPrice())).append(" 🔥\n");
            } else {
                response.append("  Giá: ").append(vndFormat.format(p.getPrice())).append("\n");
            }
            
            if (p.getStock() != null && p.getStock() > 0) {
                response.append("  ✅ Còn hàng\n");
            } else {
                response.append("  ❌ Hết hàng\n");
            }
            response.append("\n");
        }
        
        response.append("Tìm kiếm \"").append(keyword).append("\" trên website T4M để xem chi tiết! 🎁");
        return response.toString();
    }
    
    /**
     * Handle product search - find and list products
     * @deprecated Use handleProductSearchAdvanced instead
     */
    @Deprecated
    private String handleProductSearch(String message) {
        String keyword = extractKeyword(message);
        
        if (keyword.isEmpty()) {
            return "Bạn đang tìm sản phẩm gì ạ? 🔍\n" +
                   "Ví dụ: 'Tìm búp bê Elsa', 'Có xe nào đẹp không'";
        }
        
        List<Product> products = productService.searchProducts(keyword, PageRequest.of(0, 8)).getContent();
        
        if (products.isEmpty()) {
            return "Xin lỗi bạn, không tìm thấy sản phẩm nào phù hợp 😢\n\n" +
                   "🏷️ Danh mục có sẵn:\n" +
                   "👸 Búp bê | 🚀 Xe | 🧩 Lego | 🔬 Khoa học\n" +
                   "⚽ Thể thao | 🎨 Nghệ thuật | 🤖 Robot | 🎲 Board Game";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("🔍 Tìm thấy ").append(products.size()).append(" sản phẩm liên quan \"")
               .append(keyword).append("\":\n\n");
        
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        
        for (Product p : products) {
            response.append("• ").append(p.getName());
            
            if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0) {
                response.append(" | ").append(vndFormat.format(p.getDiscountPrice())).append(" 🔥");
            } else {
                response.append(" | ").append(vndFormat.format(p.getPrice()));
            }
            
            if (p.getStock() != null && p.getStock() > 0) {
                response.append(" | ✅");
            } else {
                response.append(" | ❌");
            }
            response.append("\n");
        }
        
        response.append("\nTìm kiếm \"").append(keyword).append("\" trên web T4M để xem chi tiết! 🛒");
        return response.toString();
    }
    
    /**
     * Handle policy query
     */
    private String handlePolicyQuery(String message) {
        String msg = message.toLowerCase();
        
        StringBuilder response = new StringBuilder();
        response.append("📋 Chính sách T4M:\n\n");
        
        if (msg.contains("đổi") || msg.contains("trả") || msg.contains("hoàn")) {
            response.append("🔄 Đổi trả:\n");
            response.append("• Đổi trả trong 7 ngày\n");
            response.append("• Sản phẩm còn nguyên tem, chưa qua sử dụng\n");
            response.append("• Hoàn tiền 100% nếu lỗi từ nhà sản xuất\n\n");
        }
        
        if (msg.contains("giao") || msg.contains("vận chuyển") || msg.contains("ship")) {
            response.append("🚚 Giao hàng:\n");
            response.append("• Giao hàng toàn quốc 1-3 ngày\n");
            response.append("• Miễn phí ship đơn từ 300.000₫\n");
            response.append("• Đơn dưới 300K phí ship 30.000₫\n\n");
        }
        
        if (msg.contains("thanh toán")) {
            response.append("💳 Thanh toán:\n");
            response.append("• COD (Tiền mặt khi nhận hàng)\n");
            response.append("• Chuyển khoản ngân hàng\n");
            response.append("• Ví điện tử (Momo, ZaloPay)\n\n");
        }
        
        if (msg.contains("bảo hành")) {
            response.append("🛡️ Bảo hành:\n");
            response.append("• Bảo hành 6-12 tháng tùy sản phẩm\n");
            response.append("• Hỗ trợ đổi mới nếu lỗi kỹ thuật\n\n");
        }
        
        // If no specific policy found, show all
        if (!msg.contains("đổi") && !msg.contains("trả") && 
            !msg.contains("giao") && !msg.contains("thanh toán") && 
            !msg.contains("bảo hành")) {
            response.append("🔄 Đổi trả: 7 ngày, hoàn tiền 100%\n");
            response.append("🚚 Giao hàng: 1-3 ngày, miễn phí từ 300K\n");
            response.append("💳 Thanh toán: COD, Chuyển khoản, Ví điện tử\n");
            response.append("🛡️ Bảo hành: 6-12 tháng\n\n");
        }
        
        response.append("📞 Hotline: 1800-363-363 (8h-22h hàng ngày)");
        return response.toString();
    }
    
    /**
     * Extract keyword from message (remove common words)
     */
    private String extractKeyword(String message) {
        String cleaned = message.toLowerCase()
            .replaceAll("\\b(giá|bao nhiêu|tiền|tìm|có|bán|xem|search|sản phẩm|đồ chơi|về|cho|gì|không|ạ|tim|san pham|do choi)\\b", "")
            .trim()
            .replaceAll("\\s+", " ");
        
        return cleaned.isEmpty() ? "" : cleaned;
    }
    
    /**
     * Format currency in Vietnamese format
     */
    private String formatCurrency(BigDecimal price) {
        NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        return vndFormat.format(price);
    }
    
    // ==================== END RULE-BASED HANDLERS ====================
    
    /**
     * Build product context from database for AI to have real-time information
     * SMART LOADING: Only load category overview first, then load specific products when needed
     */
    private String buildProductContext() {
        return buildCategoryOverview();
    }
    
    /**
     * Build category overview (ultra lightweight for first message)
     */
    private String buildCategoryOverview() {
        try {
            StringBuilder context = new StringBuilder();
            context.append("DANH MỤC:\n");
            
            List<Category> categories = categoryRepository.findAll();
            
            if (!categories.isEmpty()) {
                for (Category cat : categories) {
                    context.append(cat.getIcon()).append(" ").append(cat.getName()).append(" | ");
                }
            }
            
            context.append("\n\nHỏi sở thích khách để gợi ý!\n");
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error building category overview", e);
            return "";
        }
    }
    
    /**
     * Build detailed product list for specific category keywords
     */
    private String buildProductsByKeywords(String userMessage) {
        try {
            StringBuilder context = new StringBuilder();
            NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
            
            // Get all categories
            List<Category> categories = categoryRepository.findAll();
            int totalProducts = 0;
            
            // Keyword mapping for Vietnamese
            String msg = userMessage.toLowerCase();
            
            if (!categories.isEmpty()) {
                for (Category cat : categories) {
                    boolean isRelevant = false;
                    String catName = cat.getName().toLowerCase();
                    
                    // Check if category is relevant to user's message
                    if (msg.contains("búp bê") || msg.contains("công chúa") || msg.contains("barbie") || msg.contains("elsa")) {
                        isRelevant = catName.contains("búp bê") || catName.contains("công chúa");
                    } else if (msg.contains("xe") || msg.contains("ô tô") || msg.contains("phi thuyền") || msg.contains("máy bay") || msg.contains("tàu")) {
                        isRelevant = catName.contains("xe") || catName.contains("phi thuyền");
                    } else if (msg.contains("xếp hình") || msg.contains("lego") || msg.contains("ghép") || msg.contains("puzzle")) {
                        isRelevant = catName.contains("xếp hình") || catName.contains("ghép");
                    } else if (msg.contains("khoa học") || msg.contains("thí nghiệm") || msg.contains("stem") || msg.contains("kính hiển vi") || msg.contains("kính thiên văn")) {
                        isRelevant = catName.contains("khoa học") || catName.contains("thí nghiệm");
                    } else if (msg.contains("ngoài trời") || msg.contains("thể thao") || msg.contains("bóng") || msg.contains("xe đạp")) {
                        isRelevant = catName.contains("ngoài trời") || catName.contains("thể thao");
                    } else if (msg.contains("nghệ thuật") || msg.contains("sáng tạo") || msg.contains("vẽ") || msg.contains("màu")) {
                        isRelevant = catName.contains("nghệ thuật") || catName.contains("sáng tạo");
                    } else if (msg.contains("robot") || msg.contains("điện tử") || msg.contains("drone") || msg.contains("lập trình")) {
                        isRelevant = catName.contains("điện tử") || catName.contains("robot");
                    } else if (msg.contains("board game") || msg.contains("trí tuệ") || msg.contains("cờ")) {
                        isRelevant = catName.contains("board game") || catName.contains("trí tuệ");
                    }
                    
                    // Load products for relevant categories (limit to 10 products for token efficiency)
                    if (isRelevant) {
                        List<Product> products = productService.getProductsByCategory(cat.getId(), PageRequest.of(0, 10)).getContent();
                        
                        if (!products.isEmpty()) {
                            context.append("📦 ").append(cat.getName().toUpperCase()).append(":\n");
                            
                            for (Product p : products) {
                                totalProducts++;
                                context.append("  ").append(p.getName()).append(" ");
                                
                                // Price with sale info (ultra compact)
                                if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0) {
                                    context.append(vndFormat.format(p.getDiscountPrice())).append("💰");
                                } else {
                                    context.append(vndFormat.format(p.getPrice()));
                                }
                                
                                // Stock status (icon only)
                                if (p.getStock() != null && p.getStock() > 0) {
                                    context.append(" ✓");
                                } else if (p.getStock() != null) {
                                    context.append(" ✗");
                                }
                                
                                context.append("\n");
                            }
                            context.append("\n");
                        }
                    }
                }
            }
            
            if (totalProducts > 0) {
                context.insert(0, "SẢN PHẨM:\n\n");
                context.append("\n✓=Còn | ✗=Hết | 💰=SALE\nGỢI Ý 3-4 SP TỐT NHẤT với lý do!\n");
            } else {
                // No matching products, load HOT products as fallback
                context.append(buildHotProducts());
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error building products by keywords", e);
            return buildHotProducts(); // Fallback to hot products on error
        }
    }
    
    /**
     * Build HOT products list (SALE + In Stock) as fallback when no category matches
     */
    private String buildHotProducts() {
        try {
            StringBuilder context = new StringBuilder();
            NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
            
            context.append("SẢN PHẨM HOT (SALE):\n\n");
            
            // Get all categories and find products with SALE
            List<Category> categories = categoryRepository.findAll();
            int hotProductCount = 0;
            
            for (Category cat : categories) {
                if (hotProductCount >= 10) break; // Limit to 10 hot products
                
                List<Product> products = productService.getProductsByCategory(cat.getId(), PageRequest.of(0, 10)).getContent();
                
                for (Product p : products) {
                    if (hotProductCount >= 10) break;
                    
                    // Only show products with SALE and in stock
                    if (p.getDiscountPrice() != null && 
                        p.getDiscountPrice().compareTo(p.getPrice()) < 0 &&
                        p.getStock() != null && p.getStock() > 0) {
                        
                        hotProductCount++;
                        context.append("  ").append(p.getName()).append(" ");
                        context.append(vndFormat.format(p.getDiscountPrice())).append(" 💰✓\n");
                    }
                }
            }
            
            if (hotProductCount == 0) {
                // No SALE products, just show any in-stock products
                context.setLength(0);
                context.append("SẢN PHẨM PHỔ BIẾN:\n\n");
                
                for (Category cat : categories) {
                    if (hotProductCount >= 10) break;
                    
                    List<Product> products = productService.getProductsByCategory(cat.getId(), PageRequest.of(0, 5)).getContent();
                    
                    for (Product p : products) {
                        if (hotProductCount >= 10) break;
                        
                        if (p.getStock() != null && p.getStock() > 0) {
                            hotProductCount++;
                            context.append("  ").append(p.getName()).append(" ");
                            context.append(vndFormat.format(p.getPrice())).append(" ✓\n");
                        }
                    }
                }
            }
            
            context.append("\n✓=Còn | 💰=SALE\nGỢI Ý 3-4 SP TỐT NHẤT!\n");
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error building hot products", e);
            return "";
        }
    }
    
    /**
     * Call Gemini API with retry mechanism for 503 errors
     */
    private ResponseEntity<String> callGeminiAPIWithRetry(String apiUrl, HttpEntity<Map<String, Object>> entity) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            attempt++;
            
            try {
                logger.info("Calling Gemini API (attempt {}/{})", attempt, MAX_RETRIES);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
                );
                
                logger.info("Gemini API call successful on attempt {}", attempt);
                return response;
                
            } catch (HttpServerErrorException.ServiceUnavailable e) {
                lastException = e;
                logger.warn("Gemini API overloaded (503) on attempt {}. Message: {}", 
                           attempt, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        logger.info("Waiting {} ms before retry...", RETRY_DELAY_MS);
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            } catch (Exception e) {
                // For other exceptions, don't retry
                logger.error("Gemini API error (non-retryable): {}", e.getMessage());
                throw e;
            }
        }
        
        // All retries failed
        logger.error("All {} retry attempts failed. Last error: {}", 
                    MAX_RETRIES, lastException != null ? lastException.getMessage() : "unknown");
        throw new RuntimeException("Gemini API is overloaded after " + MAX_RETRIES + " attempts. Please try again later.", 
                                  lastException);
    }
}
