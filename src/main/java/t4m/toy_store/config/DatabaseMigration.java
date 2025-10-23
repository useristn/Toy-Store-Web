package t4m.toy_store.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Database migration để cập nhật enum status thêm FAILED
 * Chạy tự động khi app khởi động
 */
@Component
@Order(0) // Chạy đầu tiên, trước các bean khác
@RequiredArgsConstructor
public class DatabaseMigration implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigration.class);
    
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        logger.info("🔧 Checking database schema...");
        
        try {
            // Kiểm tra xem enum đã có FAILED chưa
            String checkSql = "SHOW COLUMNS FROM orders WHERE Field = 'status'";
            String currentEnum = jdbcTemplate.queryForObject(checkSql, (rs, num) -> rs.getString("Type"));
            
            if (currentEnum != null && !currentEnum.contains("FAILED")) {
                logger.warn("⚠️ Enum status chưa có FAILED, đang cập nhật...");
                
                // Cập nhật enum thêm FAILED
                String alterSql = """
                    ALTER TABLE orders 
                    MODIFY COLUMN status ENUM(
                        'PENDING', 
                        'CONFIRMED', 
                        'PROCESSING', 
                        'SHIPPING', 
                        'DELIVERED', 
                        'FAILED', 
                        'CANCELLED', 
                        'REFUNDED'
                    ) NOT NULL DEFAULT 'PENDING'
                    """;
                
                jdbcTemplate.execute(alterSql);
                logger.info("✅ Đã cập nhật enum status thành công! Thêm FAILED vào database.");
            } else {
                logger.info("✅ Database schema đã up-to-date, enum FAILED đã tồn tại.");
            }
            
        } catch (Exception e) {
            logger.error("❌ Lỗi khi cập nhật database schema: {}", e.getMessage());
            // Không throw exception để app vẫn chạy được
        }
    }
}
