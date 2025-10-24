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

            // Tạo bảng support_session nếu chưa tồn tại
            createSupportTables();

        } catch (Exception e) {
            logger.error("❌ Lỗi khi cập nhật database schema: {}", e.getMessage());
            // Không throw exception để app vẫn chạy được
        }
    }

    private void createSupportTables() {
        try {
            // Tạo bảng support_session
            String createSessionTable = """
                    CREATE TABLE IF NOT EXISTS support_session (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        session_id VARCHAR(255) UNIQUE NOT NULL,
                        user_id BIGINT,
                        user_email VARCHAR(255),
                        user_name VARCHAR(255),
                        status VARCHAR(50) DEFAULT 'ACTIVE',
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        unread_count INT DEFAULT 0,
                        INDEX idx_session_id (session_id),
                        INDEX idx_user_id (user_id),
                        INDEX idx_status (status)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;

            jdbcTemplate.execute(createSessionTable);
            logger.info("✅ Đã tạo/kiểm tra bảng support_session");

            // Tạo bảng support_message
            String createMessageTable = """
                    CREATE TABLE IF NOT EXISTS support_message (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        session_id VARCHAR(255) NOT NULL,
                        user_id BIGINT,
                        user_email VARCHAR(255),
                        user_name VARCHAR(255),
                        sender_type VARCHAR(50) NOT NULL,
                        message TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        is_read BOOLEAN DEFAULT FALSE,
                        INDEX idx_session_id (session_id),
                        INDEX idx_created_at (created_at),
                        INDEX idx_sender_type (sender_type),
                        FOREIGN KEY (session_id) REFERENCES support_session(session_id) ON DELETE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                    """;

            jdbcTemplate.execute(createMessageTable);
            logger.info("✅ Đã tạo/kiểm tra bảng support_message");

        } catch (Exception e) {
            logger.error("❌ Lỗi khi tạo bảng support: {}", e.getMessage());
        }
    }
}
