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
    
    // Store conversation history (conversationId -> list of messages)
    // In production, use Redis or database for scalability
    private final Map<String, List<Map<String, String>>> conversationHistory = new HashMap<>();
    
    // Base system prompt for children's toy store (COMPACT VERSION - LINE BREAK)
    private static final String BASE_SYSTEM_PROMPT = 
        "Bạn là AI tư vấn đồ chơi T4M cho trẻ em. Phong cách: thân thiện, vui vẻ, ngắn gọn.\n\n" +
        "QUY TRÌNH:\n" +
        "1. Hỏi tư vấn quà → CHỈ HỎI: 'Bé là con trai hay con gái ạ?'\n" +
        "2. Sau biết giới tính → CHỈ HỎI: 'Bé thích loại đồ chơi nào ạ?' (đưa gợi ý ngắn)\n" +
        "3. Sau biết sở thích:\n" +
        "   - NẾU có sản phẩm phù hợp TRONG DANH SÁCH → GỢI Ý 3-4 sản phẩm\n" +
        "   - NẾU KHÔNG có sản phẩm phù hợp TRONG DANH SÁCH → BẮT BUỘC trả lời:\n\n" +
        "     'Hiện tại cửa hàng T4M chưa có về loại sản phẩm này ạ. Tôi sẽ gợi ý cho bạn một vài mẫu sản phẩm đang hot bên tôi.'\n\n" +
        "     (sau đó gợi ý 3-4 sản phẩm từ danh sách SẢN PHẨM HOT)\n\n" +
        "4. KHI KHÁCH CHỌN SẢN PHẨM (nói tên sản phẩm hoặc 'tôi chọn...'):\n" +
        "   → BẮT BUỘC trả lời CHÍNH XÁC:\n\n" +
        "   'Cảm ơn bạn đã chọn <TÊN SẢN PHẨM>! 🎁\n\n" +
        "   Bạn hãy tìm kiếm \"<TÊN SẢN PHẨM>\" trên web T4M của chúng tôi để có thông tin chi tiết về sản phẩm này.\n\n" +
        "   Chúc bạn có một trải nghiệm mua sắm tuyệt vời trên cửa hàng T4M! 😊'\n\n" +
        "   ⚠️ FORMAT BẮT BUỘC:\n" +
        "   - MỖI SẢN PHẨM MỘT DÒNG (xuống dòng sau mỗi sản phẩm)\n" +
        "   - Format từng dòng: • Tên | Trạng thái | Mô tả\n" +
        "   - KHÔNG ghi giá tiền, KHÔNG gộp nhiều sản phẩm trên 1 dòng\n\n" +
        "   VÍ DỤ ĐÚNG:\n" +
        "   Tuyệt vời! T4M có gợi ý:\n" +
        "   • Búp bê Elsa | Còn hàng, SALE | Công chúa Elsa xinh đẹp!\n" +
        "   • Búp bê Barbie | Còn hàng | Ngôi nhà mơ ước của Barbie!\n" +
        "   • Búp bê Jasmine | Còn hàng, SALE | Công chúa Jasmine quyến rũ!\n" +
        "   Bạn chọn món nào ạ?\n\n" +
        "   VÍ DỤ SAI (TUYỆT ĐỐI TRÁNH):\n" +
        "   • Búp bê Elsa | Còn hàng | Xinh đẹp! • Búp bê Barbie | Còn hàng | Ngôi nhà!\n\n" +
        "LƯU Ý: MỖI LẦN CHỈ HỎI 1 CÂU | Trả lời NGẮN GỌN | ÍT EMOJI | CHỈ gợi ý sản phẩm CÓ TRONG DANH SÁCH\n\n" +
        "CHÍNH SÁCH: Đổi trả 7 ngày | Giao 1-3 ngày | Miễn phí từ 300K | Hotline: 1800-363-363\n\n";
    
    public String generateResponse(String userMessage, String conversationId) {
        logger.info("=== ChatbotService.generateResponse CALLED ===");
        logger.info("User message: {}, Conversation ID: {}", userMessage, conversationId);
        logger.info("Gemini API key configured: {}", geminiApiKey != null && !geminiApiKey.isEmpty());
        
        if (geminiApiKey == null || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            logger.warn("Gemini API key not configured");
            return "Xin lỗi bạn, chatbot AI chưa được cấu hình. Vui lòng liên hệ quản trị viên hoặc gọi hotline 1800-363-363 để được hỗ trợ! 😊";
        }
        
        try {
            // Get or create conversation history
            List<Map<String, String>> history = conversationHistory.computeIfAbsent(conversationId, k -> new ArrayList<>());
            
            // SMART CONTEXT LOADING: Load products based on conversation stage
            String productContext = "";
            
            if (history.isEmpty()) {
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
