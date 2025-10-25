package t4m.toy_store.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Intent Classification - Step 2: Natural Language Processing
 * Phân loại mục đích và thu thập thông số từ câu hỏi của khách hàng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentClassification {
    
    // Intent types for toy e-commerce
    public enum Intent {
        PRODUCT_RECOMMENDATION,  // Tư vấn sản phẩm
        PRODUCT_SEARCH,          // Tìm kiếm sản phẩm cụ thể
        PRICE_INQUIRY,           // Hỏi giá
        AVAILABILITY_CHECK,      // Kiểm tra còn hàng
        POLICY_INQUIRY,          // Hỏi chính sách (đổi trả, giao hàng)
        AGE_RECOMMENDATION,      // Tư vấn theo độ tuổi
        CATEGORY_BROWSE,         // Xem danh mục
        PROMOTION_INQUIRY,       // Hỏi khuyến mãi
        ORDER_TRACKING,          // Theo dõi đơn hàng
        COMPLAINT,               // Khiếu nại
        GENERAL_INQUIRY,         // Câu hỏi chung
        HUMAN_HANDOFF           // Yêu cầu chuyển nhân viên
    }
    
    private Intent intent;
    private double confidence; // 0.0 - 1.0
    
    // Slots - extracted parameters
    private Map<String, Object> slots = new HashMap<>();
    
    // Helper methods to set slots
    public void setSlot(String key, Object value) {
        slots.put(key, value);
    }
    
    public Object getSlot(String key) {
        return slots.get(key);
    }
    
    public String getSlotAsString(String key) {
        Object value = slots.get(key);
        return value != null ? value.toString() : null;
    }
    
    public Integer getSlotAsInteger(String key) {
        Object value = slots.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    // Common slot keys
    public static final String SLOT_CHILD_GENDER = "child_gender";      // "boy" / "girl"
    public static final String SLOT_CHILD_AGE = "child_age";            // Integer
    public static final String SLOT_CATEGORY = "category";              // String
    public static final String SLOT_PRODUCT_NAME = "product_name";      // String
    public static final String SLOT_PRICE_RANGE = "price_range";        // "low" / "medium" / "high"
    public static final String SLOT_OCCASION = "occasion";              // "birthday" / "holiday" / "reward"
    public static final String SLOT_INTERESTS = "interests";            // String or List<String>
}
