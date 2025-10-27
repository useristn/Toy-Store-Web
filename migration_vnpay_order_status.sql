-- Migration to support new PENDING_PAYMENT status for VNPay integration
-- Run this SQL before starting the application

USE toy_store;

-- Modify status column to accommodate longer enum values
ALTER TABLE orders MODIFY COLUMN status VARCHAR(20) NOT NULL;

-- Update existing orders description if needed
-- (No data migration needed as PENDING_PAYMENT is a new status)

-- Verify the change
DESCRIBE orders;

SELECT 'Migration completed successfully!' AS message;
