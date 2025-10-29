package t4m.toy_store.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import t4m.toy_store.chatbot.dto.ConversationState;
import t4m.toy_store.chatbot.dto.IntentClassification;
import t4m.toy_store.product.entity.Category;
import t4m.toy_store.product.entity.Product;
import t4m.toy_store.product.repository.CategoryRepository;
import t4m.toy_store.product.service.ProductService;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Deque;

@Service
@RequiredArgsConstructor
public class ChatbotService {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotService.class);
    
    // Retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000; // 2 seconds
    private static final int MAX_HISTORY_SIZE = 20; // Limit conversation history
    private static final int MAX_CONVERSATIONS = 10000; // Global limit
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    
    // NEW: Professional services for 7-step flow
    private final IntentRecognitionService intentRecognitionService;
    private final InteractionLoggingService interactionLoggingService;
    
    // IMPROVED: Thread-safe conversation history with bounded size
    private final Map<String, Deque<Map<String, String>>> conversationHistory = new ConcurrentHashMap<>();
    
    // Base system prompt for children's toy store (COMPLETE PRODUCT CATALOG)
    private static final String BASE_SYSTEM_PROMPT = 
        "Bạn là AI tư vấn đồ chơi T4M cho trẻ em. Phong cách: thân thiện, vui vẻ, ngắn gọn.\n\n" +
        
        "📚 KIẾN THỨC SẢN PHẨM CỬA HÀNG T4M (8 DANH MỤC - 100 SẢN PHẨM):\n\n" +
        
        "👸 1. BÚP BÊ & CÔNG CHÚA (12 sản phẩm):\n" +
        "• Búp bê Công chúa Elsa - Công chúa băng giá xinh đẹp với bộ váy lung linh\n" +
        "• Búp bê Anna cổ tích - Công chúa dũng cảm với trang phục đẹp mắt\n" +
        "• Búp bê Barbie Dream House - Búp bê Barbie sang trọng với ngôi nhà mơ ước\n" +
        "• Búp bê Ariel nàng tiên cá - Nàng tiên cá xinh đẹp với đuôi cá lấp lánh\n" +
        "• Búp bê Belle người đẹp - Công chúa Belle yêu đọc sách\n" +
        "• Búp bê Jasmine công chúa - Công chúa Jasmine với trang phục Ả Rập\n" +
        "• Búp bê Cinderella lọ lem - Công chúa Lọ Lem với giày thủy tinh\n" +
        "• Set búp bê gia đình hạnh phúc - Bộ búp bê gia đình 4 người\n" +
        "• Búp bê baby doll - Em bé búp bê biết khóc, cười\n" +
        "• Búp bê LOL Surprise - Búp bê bất ngờ với nhiều phụ kiện\n" +
        "• Set búp bê Disney Princess - Bộ 5 công chúa Disney\n" +
        "• Búp bê Aurora ngủ trong rừng - Công chúa ngủ trong rừng xinh đẹp\n\n" +
        
        "🚀 2. XE & PHI THUYỀN (15 sản phẩm):\n" +
        "• Phi thuyền Siêu tốc X-Wing - Phi thuyền chiến đấu tốc độ ánh sáng\n" +
        "• Xe ô tô điều khiển từ xa - Xe đua điều khiển tốc độ cao\n" +
        "• Tàu vũ trụ Apollo - Tàu vũ trụ Apollo mô hình chi tiết\n" +
        "• Xe tăng chiến đấu - Xe tăng quân sự điều khiển\n" +
        "• Máy bay phản lực F-16 - Máy bay chiến đấu F-16 mô hình\n" +
        "• Xe cứu hỏa siêu tốc - Xe cứu hỏa với thang cứu nạn\n" +
        "• Xe cảnh sát tuần tra - Xe cảnh sát với còi hú\n" +
        "• Xe đua F1 Lightning - Xe đua F1 tốc độ siêu nhanh\n" +
        "• Tên lửa Falcon Heavy - Tên lửa SpaceX Falcon Heavy\n" +
        "• Máy bay trực thăng - Trực thăng cứu hộ điều khiển\n" +
        "• Tàu hỏa cao tốc Bullet - Tàu hỏa siêu tốc Nhật Bản\n" +
        "• Xe mô tô đua Ducati - Mô tô đua Ducati tốc độ\n" +
        "• Phi thuyền Millennium Falcon - Phi thuyền huyền thoại Star Wars\n" +
        "• Tàu cướp biển Caribbean - Tàu cướp biển với cờ đầu lâu\n" +
        "• Set phương tiện cứu hộ - Bộ 5 xe cứu hộ khẩn cấp\n\n" +
        
        "🧩 3. XẾP HÌNH & GHÉP (12 sản phẩm):\n" +
        "• Lego City Trung tâm vũ trụ - Bộ xếp hình trung tâm vũ trụ NASA 1000 chi tiết\n" +
        "• Lego Technic siêu xe - Xếp hình siêu xe Lamborghini\n" +
        "• Puzzle 1000 mảnh thiên hà - Tranh ghép hình thiên hà đẹp mắt\n" +
        "• Lego Friends công viên giải trí - Công viên vui chơi với nhiều trò chơi\n" +
        "• Minecraft thế giới khối vuông - Bộ xếp hình Minecraft 500 chi tiết\n" +
        "• Lego Harry Potter lâu đài - Lâu đài Hogwarts huyền thoại\n" +
        "• Puzzle 3D tháp Eiffel - Puzzle 3D tháp Eiffel Paris 216 mảnh\n" +
        "• Rubik's Cube 3x3 tốc độ - Rubik cube tốc độ chuyên nghiệp\n" +
        "• Lego Disney lâu đài công chúa - Lâu đài Disney Princess tuyệt đẹp\n" +
        "• Khối nam châm Magformers - Bộ khối nam châm ghép hình 50 chi tiết\n" +
        "• Lego Jurassic World khủng long - Bộ xếp hình khủng long T-Rex\n" +
        "• Lego Duplo trang trại vui vẻ - Bộ xếp hình trang trại cho bé nhỏ\n\n" +
        
        "🔬 4. KHOA HỌC & THÍ NGHIỆM (10 sản phẩm):\n" +
        "• Bộ thí nghiệm Vũ trụ 100 thí nghiệm - Khám phá 100 thí nghiệm khoa học tuyệt vời\n" +
        "• Kính thiên văn khám phá sao - Kính thiên văn chuyên nghiệp 70mm\n" +
        "• Bộ hóa học nhỏ - Thí nghiệm hóa học an toàn cho trẻ em\n" +
        "• Kính hiển vi sinh học - Kính hiển vi học sinh 1200x\n" +
        "• Robot lập trình STEM - Robot học lập trình cho trẻ em\n" +
        "• Bộ thí nghiệm núi lửa - Tạo núi lửa phun trào tại nhà\n" +
        "• Mô hình hệ mặt trời - Hệ mặt trời quay tự động có đèn\n" +
        "• Bộ thí nghiệm điện từ - Khám phá điện và từ trường\n" +
        "• Bộ khai quật hóa thạch khủng long - Khám phá hóa thạch như nhà khảo cổ\n" +
        "• Kit Arduino cho trẻ em - Học lập trình điện tử cơ bản\n\n" +
        
        "⚽ 5. NGOÀI TRỜI & THỂ THAO (12 sản phẩm):\n" +
        "• Bóng đá World Cup 2024 - Bóng đá chính thức World Cup size 5\n" +
        "• Xe đạp thể thao trẻ em - Xe đạp 16 inch cho bé 5-8 tuổi\n" +
        "• Bóng rổ NBA Professional - Bóng rổ cao cấp size 7\n" +
        "• Ván trượt Skateboard Pro - Ván trượt chuyên nghiệp 7 lớp\n" +
        "• Xe scooter 3 bánh - Xe scooter phát sáng cho bé\n" +
        "• Bộ cầu lông gia đình - Set cầu lông 4 vợt kèm lưới\n" +
        "• Bóng tennis Wilson - Bộ 3 bóng tennis chuyên nghiệp\n" +
        "• Ván trượt patin Rollerblade - Giày trượt patin 8 bánh\n" +
        "• Bộ bóng bàn Di Động - Set bóng bàn gắn mọi bàn\n" +
        "• Bể bơi phao gia đình - Bể bơi phao 3m x 2m\n" +
        "• Xe trượt Hoverboard - Xe điện cân bằng 2 bánh\n" +
        "• Set bơi lội kính + ống thở - Bộ lặn snorkel cho trẻ em\n\n" +
        
        "🎨 6. NGHỆ THUẬT & SÁNG TẠO (13 sản phẩm):\n" +
        "• Bộ màu nước 36 màu - Màu nước chuyên nghiệp kèm cọ\n" +
        "• Bàn vẽ điện tử LCD - Bảng vẽ điện tử xóa được 8.5 inch\n" +
        "• Bộ sáp màu 48 màu - Sáp màu cao cấp Crayola\n" +
        "• Bộ đất sét Play-Doh 12 hộp - Đất nặn nhiều màu sắc\n" +
        "• Máy chiếu vẽ Projector - Máy chiếu hình vẽ cho bé tập\n" +
        "• Bộ tạo vòng tay hạt - Set làm vòng tay 500 hạt màu\n" +
        "• Bộ vẽ tranh cát màu - Tranh cát 10 mẫu kèm cát màu\n" +
        "• Bộ sơn dầu 24 màu - Màu sơn dầu chuyên nghiệp\n" +
        "• Máy móc giấy Origami - 300 tờ giấy xếp hình màu\n" +
        "• Bộ làm slime galaxy - Kit tạo slime thiên hà lấp lánh\n" +
        "• Bộ vẽ tranh số Paint by Numbers - Tranh tô theo số kèm màu\n" +
        "• Bộ làm trang sức resin - Kit đổ resin làm trang sức\n" +
        "• Bộ vẽ tranh 3D Pen - Bút vẽ 3D kèm 10 màu nhựa\n\n" +
        
        "🤖 7. ĐIỆN TỬ & ROBOT (13 sản phẩm):\n" +
        "• Robot AI thông minh Cozmo - Robot AI tương tác cảm xúc\n" +
        "• Drone camera 4K trẻ em - Drone điều khiển có camera\n" +
        "• Robot biến hình Transformer - Robot biến thành xe hơi\n" +
        "• Đồng hồ thông minh trẻ em - Smartwatch GPS cho bé\n" +
        "• Robot khủng long điều khiển - Khủng long robot phun khói\n" +
        "• Bộ mạch Arduino Starter Kit - Kit học lập trình Arduino\n" +
        "• Robot lắp ráp Makeblock - Robot DIY lập trình được\n" +
        "• Máy chơi game cầm tay retro - 500 game kinh điển tích hợp\n" +
        "• Robot chó cảm biến - Chó robot biết đi, sủa, vẫy đuôi\n" +
        "• Bộ mạch Raspberry Pi 4 - Máy tính nhỏ học lập trình\n" +
        "• Bộ thí nghiệm điện tử 100in1 - 100 mạch điện tử thí nghiệm\n" +
        "• Robot biến hình 5in1 - 1 robot biến thành 5 hình\n" +
        "• Robot lắp ghép sáng tạo - 500 chi tiết lắp tự do\n\n" +
        
        "🎲 8. BOARD GAME & TRÍ TUỆ (13 sản phẩm):\n" +
        "• Cờ tỷ phú Monopoly Việt Nam - Monopoly phiên bản Việt Nam\n" +
        "• Uno cards phiên bản đặc biệt - Bài UNO 108 lá nhiều hiệu ứng\n" +
        "• Cờ vua nam châm cao cấp - Bàn cờ vua gỗ từ tính 32cm\n" +
        "• Jenga tháp gỗ rút thanh - 54 thanh gỗ thử thách\n" +
        "• Scrabble ghép chữ tiếng Anh - Trò chơi ghép từ học Anh văn\n" +
        "• Cluedo phá án bí ẩn - Trò chơi trinh thám hấp dẫn\n" +
        "• Cờ cá ngựa 6 người chơi - Bàn cờ cá ngựa gia đình\n" +
        "• Domino 100 quân gỗ màu - Domino gỗ xếp hình sáng tạo\n" +
        "• Bài Poker cao cấp PVC - Bộ bài Poker chống nước\n" +
        "• Rubik's Cube 4x4 Revenge - Rubik 4x4 cao cấp tốc độ\n" +
        "• Mê cung 3D Perplexus - Bóng mê cung 3D 100 chướng ngại\n" +
        "• Catan Settlers of Catan - Trò chơi chiến lược phát triển\n" +
        "• Bộ bài Tây 52 lá plastic - Bài nhựa cao cấp chống nước\n\n" +
        
        "QUY TRÌNH TƯ VẤN:\n" +
        "1. Hỏi tư vấn quà → CHỈ HỎI: 'Bé là con trai hay con gái ạ?'\n" +
        "2. Sau biết giới tính → CHỈ HỎI: 'Bé thích loại đồ chơi nào ạ?' (đưa gợi ý ngắn)\n" +
        "3. Sau biết sở thích:\n" +
        "   - NẾU có sản phẩm phù hợp → GỢI Ý 3-4 sản phẩm từ DANH SÁCH TRÊN\n" +
        "   - NẾU KHÔNG có sản phẩm phù hợp → BẮT BUỘC trả lời:\n" +
        "     'Hiện tại cửa hàng T4M chưa có về loại sản phẩm này ạ. Tôi sẽ gợi ý cho bạn một vài mẫu sản phẩm đang hot.'\n" +
        "4. KHI KHÁCH CHỌN SẢN PHẨM (nói tên sản phẩm):\n" +
        "   → BẮT BUỘC trả lời: 'Cảm ơn bạn đã chọn <TÊN>! 🎁 Bạn hãy tìm kiếm \"<TÊN>\" trên web T4M để xem chi tiết. Chúc bạn mua sắm vui vẻ! 😊'\n\n" +
        
        "⚠️ FORMAT BẮT BUỘC:\n" +
        "- MỖI SẢN PHẨM MỘT DÒNG (xuống dòng sau mỗi sản phẩm)\n" +
        "- Format: • Tên | Trạng thái | Mô tả\n" +
        "- KHÔNG ghi giá tiền, KHÔNG gộp nhiều sản phẩm trên 1 dòng\n\n" +
        
        "VÍ DỤ ĐÚNG:\n" +
        "Tuyệt vời! T4M có gợi ý:\n" +
        "• Búp bê Elsa | Còn hàng, SALE | Công chúa băng giá xinh đẹp!\n" +
        "• Búp bê Barbie Dream House | Còn hàng | Ngôi nhà mơ ước!\n" +
        "• Búp bê Jasmine công chúa | Còn hàng, SALE | Trang phục Ả Rập!\n" +
        "Bạn chọn món nào ạ?\n\n" +
        
        "LƯU Ý: MỖI LẦN CHỈ HỎI 1 CÂU | Trả lời NGẮN GỌN | ÍT EMOJI | CHỈ gợi ý sản phẩm CÓ TRONG DANH SÁCH\n\n" +
        "CHÍNH SÁCH: Đổi trả 7 ngày | Giao 1-3 ngày | Miễn phí từ 300K | Hotline: 1800-8080\n\n";
    
    public String generateResponse(String userMessage, String conversationId) {
        logger.info("=== STEP 1: INPUT COLLECTION - ChatbotService.generateResponse CALLED ===");
        logger.info("User message: {}, Conversation ID: {}", userMessage, conversationId);
        logger.info("Gemini API key configured: {}", geminiApiKey != null && !geminiApiKey.isEmpty());
        
        // Step 6: Security check - API key validation
        if (geminiApiKey == null || geminiApiKey.equals("YOUR_GEMINI_API_KEY_HERE")) {
            logger.warn("SECURITY: Gemini API key not configured");
            return "Xin lỗi bạn, chatbot AI chưa được cấu hình. Vui lòng liên hệ quản trị viên hoặc gọi hotline 1800-8080 để được hỗ trợ! 😊";
        }
        
        try {
            // Step 5: Get or create conversation state for learning
            ConversationState state = interactionLoggingService.getOrCreateState(conversationId);
            state.incrementMessageCount();
            
            // Get conversation history (thread-safe with bounded size)
            Deque<Map<String, String>> history = conversationHistory.computeIfAbsent(
                conversationId, 
                id -> new ConcurrentLinkedDeque<>()
            );
            
            // Step 2: NLP & Intent Recognition
            String conversationContext = buildConversationContextString(history);
            IntentClassification intent = intentRecognitionService.classifyIntent(userMessage, conversationContext);
            interactionLoggingService.logIntent(intent.getIntent().toString());
            
            logger.info("STEP 2: Intent detected: {} (confidence: {})", intent.getIntent(), intent.getConfidence());
            logger.info("STEP 2: Extracted slots: {}", intent.getSlots());
            
            // Step 6: Check if handoff needed
            if (intent.getIntent() == IntentClassification.Intent.HUMAN_HANDOFF || 
                intent.getIntent() == IntentClassification.Intent.COMPLAINT) {
                interactionLoggingService.logHandoffRequest(conversationId, intent.getIntent().toString());
                state.setCurrentStage(ConversationState.ConversationStage.AWAITING_HANDOFF);
                return buildHandoffResponse();
            }
            
            // Step 3: Toy-specific data retrieval and logic
            String productContext = buildToySpecificContext(history, userMessage, intent, state);
            
            // Add user message to history
            addToHistory(conversationId, history, "user", userMessage);
            
            // Build conversation context from history (keep it SHORT)
            StringBuilder conversationContextBuilder = new StringBuilder();
            // Only include recent messages (last 6 messages = 3 turns)
            int count = 0;
            Iterator<Map<String, String>> iterator = ((ConcurrentLinkedDeque<Map<String, String>>) history).descendingIterator();
            while (iterator.hasNext() && count < 6) {
                Map<String, String> msg = iterator.next();
                
                if ("user".equals(msg.get("role"))) {
                    conversationContextBuilder.insert(0, "Khách: " + msg.get("message") + "\n");
                } else if ("assistant".equals(msg.get("role"))) {
                    conversationContextBuilder.insert(0, "AI: " + msg.get("message") + "\n");
                }
                count++;
            }
            
            // Step 4: Build prompt and generate response
            String fullPrompt = buildPromptForIntent(intent, productContext, conversationContextBuilder.toString(), userMessage, history.size());
            
            logger.info("STEP 4: Prompt length: {} chars (~{} tokens), History: {} msgs", 
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
            generationConfig.put("maxOutputTokens", 2048);
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
                String aiResponse = parseGeminiResponse(responseBody, conversationId);
                
                if (aiResponse != null && !aiResponse.startsWith("Xin lỗi")) {
                    // Add AI response to history
                    addToHistory(conversationId, history, "assistant", aiResponse);
                    
                    // Step 5: Log successful interaction
                    interactionLoggingService.logSuccessfulRecommendation(conversationId);
                    updateConversationStage(state, intent);
                    
                    logger.info("STEP 5: Response generated successfully for conversation: {}", conversationId);
                    
                    return aiResponse;
                }
                
                return aiResponse != null ? aiResponse : "Xin lỗi bạn, mình không thể trả lời câu hỏi này ngay bây giờ. Bạn có thể thử hỏi câu khác hoặc liên hệ hotline 1800-8080 nhé! 😊";
            } else {
                logger.error("Gemini API returned status: {}, body: {}", response.getStatusCode(), response.getBody());
            }
            
            logger.warn("Unexpected response format from Gemini API. Could not extract text from response.");
            return "Xin lỗi bạn, mình không thể trả lời câu hỏi này ngay bây giờ. Bạn có thể thử hỏi câu khác hoặc liên hệ hotline 1800-8080 nhé! 😊";
            
        } catch (Exception e) {
            logger.error("Error calling Gemini API for conversation: " + conversationId, e);
            logger.error("Error details - Type: {}, Message: {}", e.getClass().getName(), e.getMessage());
            if (e.getCause() != null) {
                logger.error("Caused by: {}", e.getCause().getMessage());
            }
            
            // Check if it's an overload error
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("overloaded") || errorMsg.contains("503"))) {
                return "Xin lỗi bạn, hệ thống AI đang quá tải. Vui lòng thử lại sau 1-2 phút hoặc gọi hotline 1800-8080 để được hỗ trợ trực tiếp nhé! 😊";
            }
            
            return "Xin lỗi bạn, mình đang gặp chút trục trặc kỹ thuật. Bạn thử lại sau hoặc gọi hotline 1800-8080 để được hỗ trợ trực tiếp nhé! 😊";
        }
    }
    
    public String generateConversationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * STEP 3: Build toy-specific context based on intent and conversation stage
     */
    private String buildToySpecificContext(Deque<Map<String, String>> history, String userMessage, 
                                           IntentClassification intent, ConversationState state) {
        logger.info("STEP 3: Building toy-specific context for intent: {}", intent.getIntent());
        
        String productContext = "";
        
        switch (intent.getIntent()) {
            case PRODUCT_RECOMMENDATION:
            case AGE_RECOMMENDATION:
                if (history.isEmpty()) {
                    // First message: category overview
                    productContext = buildCategoryOverview();
                    state.setCurrentStage(ConversationState.ConversationStage.COLLECTING_CHILD_INFO);
                } else if (history.size() >= 2) {
                    // Load specific products based on preferences
                    productContext = buildProductsByIntent(intent);
                    state.setCurrentStage(ConversationState.ConversationStage.SHOWING_PRODUCTS);
                }
                break;
                
            case PRODUCT_SEARCH:
                // Direct product search
                String productName = intent.getSlotAsString(IntentClassification.SLOT_PRODUCT_NAME);
                if (productName != null) {
                    productContext = searchProductByName(productName);
                }
                break;
                
            case PROMOTION_INQUIRY:
                // Show products on sale
                productContext = buildHotProducts();
                break;
                
            case POLICY_INQUIRY:
                // No product context needed
                productContext = "";
                break;
                
            default:
                // General case - smart loading
                if (history.size() >= 2) {
                    productContext = buildProductsByKeywords(userMessage);
                }
                break;
        }
        
        logger.info("STEP 3: Product context built: {} characters", productContext.length());
        return productContext;
    }
    
    /**
     * STEP 4: Build prompt based on intent classification
     */
    private String buildPromptForIntent(IntentClassification intent, String productContext, 
                                        String conversationContext, String userMessage, int historySize) {
        logger.info("STEP 4: Building prompt for intent: {}", intent.getIntent());
        
        StringBuilder prompt = new StringBuilder();
        prompt.append(BASE_SYSTEM_PROMPT);
        
        // Add intent-specific instructions
        switch (intent.getIntent()) {
            case PRODUCT_RECOMMENDATION:
            case AGE_RECOMMENDATION:
                prompt.append("\n📌 MỤC ĐÍCH: Tư vấn sản phẩm phù hợp với độ tuổi và sở thích.\n");
                if (!productContext.isEmpty()) {
                    prompt.append(productContext);
                }
                break;
                
            case POLICY_INQUIRY:
                prompt.append("\n📌 MỤC ĐÍCH: Giải đáp về chính sách cửa hàng (ngắn gọn, rõ ràng).\n");
                break;
                
            case PROMOTION_INQUIRY:
                prompt.append("\n📌 MỤC ĐÍCH: Thông tin về khuyến mãi và sản phẩm SALE.\n");
                if (!productContext.isEmpty()) {
                    prompt.append(productContext);
                }
                break;
                
            case PRICE_INQUIRY:
                prompt.append("\n📌 MỤC ĐÍCH: Cung cấp thông tin giá sản phẩm.\n");
                break;
                
            default:
                if (!productContext.isEmpty()) {
                    prompt.append(productContext);
                }
                break;
        }
        
        // Add conversation context
        if (!conversationContext.isEmpty()) {
            prompt.append("\n\nHỘI THOẠI GẦN ĐÂY:\n").append(conversationContext);
        }
        
        // Add final instruction
        if (historySize == 1) {
            prompt.append("\n\nKhách hàng hỏi: ").append(userMessage).append("\n\nTrả lời:");
        } else {
            prompt.append("\n\nTrả lời tin nhắn mới nhất (ngắn gọn, thân thiện):");
        }
        
        return prompt.toString();
    }
    
    /**
     * STEP 6: Build handoff response when human agent needed
     */
    private String buildHandoffResponse() {
        return "Mình hiểu bạn muốn được hỗ trợ trực tiếp! 👨‍💼\n\n" +
               "Vui lòng liên hệ:\n" +
               "📞 Hotline: 1800-8080 (miễn phí)\n" +
               "⏰ Làm việc: 8h-22h hàng ngày\n" +
               "📧 Email: support@t4m.com\n\n" +
               "Hoặc bạn có thể tiếp tục hỏi mình nếu cần tư vấn sản phẩm nhé! 😊";
    }
    
    /**
     * Helper: Add message to history with bounded size (thread-safe)
     */
    private void addToHistory(String conversationId, Deque<Map<String, String>> history, 
                              String role, String message) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("message", message);
        
        history.addLast(msg);
        
        // Enforce size limit
        while (history.size() > MAX_HISTORY_SIZE) {
            history.pollFirst();
        }
        
        // Enforce global conversation limit
        if (conversationHistory.size() > MAX_CONVERSATIONS) {
            // Remove oldest conversation (simple eviction)
            Iterator<String> iterator = conversationHistory.keySet().iterator();
            if (iterator.hasNext()) {
                String oldestKey = iterator.next();
                conversationHistory.remove(oldestKey);
                logger.info("Evicted oldest conversation: {}", oldestKey);
            }
        }
    }
    
    /**
     * Helper: Build conversation context string for intent recognition
     */
    private String buildConversationContextString(Deque<Map<String, String>> history) {
        StringBuilder context = new StringBuilder();
        int count = 0;
        
        Iterator<Map<String, String>> iterator = ((ConcurrentLinkedDeque<Map<String, String>>) history).descendingIterator();
        while (iterator.hasNext() && count < 4) {
            Map<String, String> msg = iterator.next();
            context.insert(0, msg.get("role") + ": " + msg.get("message") + "\n");
            count++;
        }
        
        return context.toString();
    }
    
    /**
     * Helper: Update conversation stage based on intent
     */
    private void updateConversationStage(ConversationState state, IntentClassification intent) {
        switch (intent.getIntent()) {
            case PRODUCT_RECOMMENDATION:
            case PRODUCT_SEARCH:
                if (state.getCurrentStage() == ConversationState.ConversationStage.GREETING) {
                    state.setCurrentStage(ConversationState.ConversationStage.COLLECTING_CHILD_INFO);
                } else if (state.getCurrentStage() == ConversationState.ConversationStage.COLLECTING_CHILD_INFO) {
                    state.setCurrentStage(ConversationState.ConversationStage.COLLECTING_PREFERENCES);
                } else {
                    state.setCurrentStage(ConversationState.ConversationStage.SHOWING_PRODUCTS);
                }
                break;
                
            case HUMAN_HANDOFF:
            case COMPLAINT:
                state.setCurrentStage(ConversationState.ConversationStage.AWAITING_HANDOFF);
                break;
                
            default:
                state.setCurrentStage(ConversationState.ConversationStage.HANDLING_INQUIRY);
                break;
        }
        
        interactionLoggingService.logConversation(state.getConversationId(), state);
    }
    
    /**
     * Parse Gemini API response
     */
    private String parseGeminiResponse(String responseBody, String conversationId) {
        try {
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
                return "Xin lỗi bạn, AI không trả lời được. Vui lòng thử lại hoặc gọi hotline 1800-8080 nhé! 😊";
            }
            
            JsonNode candidates = root.get("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                
                // Check if blocked by safety filters (Step 6: Security)
                if (firstCandidate.has("finishReason")) {
                    String finishReason = firstCandidate.get("finishReason").asText();
                    if ("SAFETY".equals(finishReason)) {
                        logger.warn("SECURITY: Response blocked by safety filters for conversation: {}", conversationId);
                        return "Xin lỗi bạn, câu hỏi này không phù hợp. Bạn có thể hỏi về sản phẩm hoặc gọi hotline 1800-8080 nhé! 😊";
                    }
                }
                
                JsonNode content_node = firstCandidate.path("content");
                JsonNode parts = content_node.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String aiResponse = parts.get(0).path("text").asText();
                    
                    if (aiResponse == null || aiResponse.trim().isEmpty()) {
                        logger.error("AI response is empty. Candidate: {}", firstCandidate);
                        return "Xin lỗi bạn, mình không có câu trả lời phù hợp. Vui lòng thử lại hoặc gọi hotline 1800-8080 nhé! 😊";
                    }
                    
                    return aiResponse;
                } else {
                    logger.error("Parts array is empty or missing. Candidate: {}", firstCandidate);
                }
            } else {
                logger.error("Candidates array is empty or missing. Root: {}", root);
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Error parsing Gemini response", e);
            return null;
        }
    }
    
    /**
     * Build products by intent slots
     */
    private String buildProductsByIntent(IntentClassification intent) {
        try {
            StringBuilder context = new StringBuilder();
            NumberFormat vndFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
            
            // Extract filters from intent slots
            String category = intent.getSlotAsString(IntentClassification.SLOT_CATEGORY);
            Integer age = intent.getSlotAsInteger(IntentClassification.SLOT_CHILD_AGE);
            String gender = intent.getSlotAsString(IntentClassification.SLOT_CHILD_GENDER);
            
            logger.info("Building products for - Category: {}, Age: {}, Gender: {}", category, age, gender);
            
            List<Category> categories = categoryRepository.findAll();
            int totalProducts = 0;
            
            for (Category cat : categories) {
                // Filter by category if specified
                if (category != null && !cat.getName().toLowerCase().contains(category.toLowerCase())) {
                    continue;
                }
                
                List<Product> products = productService.getProductsByCategory(cat.getId(), PageRequest.of(0, 10)).getContent();
                
                if (!products.isEmpty()) {
                    context.append("📦 ").append(cat.getName().toUpperCase()).append(":\n");
                    
                    for (Product p : products) {
                        totalProducts++;
                        context.append("  • ").append(p.getName());
                        
                        // Stock status
                        if (p.getStock() != null && p.getStock() > 0) {
                            if (p.getDiscountPrice() != null && p.getDiscountPrice().compareTo(p.getPrice()) < 0) {
                                context.append(" | Còn hàng, SALE");
                            } else {
                                context.append(" | Còn hàng");
                            }
                        } else {
                            context.append(" | Hết hàng");
                        }
                        
                        // Description (shortened)
                        if (p.getDescription() != null && !p.getDescription().isEmpty()) {
                            String shortDesc = p.getDescription().length() > 50 ? 
                                             p.getDescription().substring(0, 47) + "..." : 
                                             p.getDescription();
                            context.append(" | ").append(shortDesc);
                        }
                        
                        context.append("\n");
                        
                        if (totalProducts >= 8) break; // Limit products
                    }
                    context.append("\n");
                    
                    if (totalProducts >= 8) break;
                }
            }
            
            if (totalProducts > 0) {
                context.insert(0, "SẢN PHẨM PHÙ HỢP:\n\n");
                context.append("\nGỢI Ý 3-4 sản phẩm TỐT NHẤT với lý do cụ thể!\n");
            } else {
                context.append(buildHotProducts());
            }
            
            return context.toString();
            
        } catch (Exception e) {
            logger.error("Error building products by intent", e);
            return buildHotProducts();
        }
    }
    
    /**
     * Search product by name
     */
    private String searchProductByName(String productName) {
        // TODO: Implement full-text search in ProductService
        logger.info("Searching for product: {}", productName);
        return buildProductsByKeywords(productName);
    }
    
    /**
     * Scheduled cleanup task (Step 5 & 7: Memory management and optimization)
     */
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void cleanupOldConversations() {
        logger.info("STEP 7: Running scheduled conversation cleanup...");
        interactionLoggingService.cleanupOldConversations(24); // 24 hours
        
        // Cleanup local history as well
        int sizeBefore = conversationHistory.size();
        if (sizeBefore > MAX_CONVERSATIONS) {
            Iterator<String> iterator = conversationHistory.keySet().iterator();
            int toRemove = sizeBefore - MAX_CONVERSATIONS;
            
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }
            
            logger.info("STEP 7: Cleaned up {} conversations. Remaining: {}", 
                       sizeBefore - conversationHistory.size(), conversationHistory.size());
        }
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
