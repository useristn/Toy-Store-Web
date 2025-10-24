-- SQL Script to update existing products with default rating values
-- Run this if you change ddl-auto from 'create' to 'update'

-- Add columns if they don't exist (for MySQL)
ALTER TABLE products 
ADD COLUMN IF NOT EXISTS average_rating DOUBLE DEFAULT 0.0,
ADD COLUMN IF NOT EXISTS rating_count INT DEFAULT 0;

-- Update existing products that have NULL values
UPDATE products 
SET average_rating = 0.0 
WHERE average_rating IS NULL;

UPDATE products 
SET rating_count = 0 
WHERE rating_count IS NULL;

-- Optional: Recalculate ratings from existing rating data
UPDATE products p
LEFT JOIN (
    SELECT 
        product_id,
        AVG(stars) as avg_rating,
        COUNT(*) as rating_count
    FROM ratings
    GROUP BY product_id
) r ON p.id = r.product_id
SET 
    p.average_rating = COALESCE(r.avg_rating, 0.0),
    p.rating_count = COALESCE(r.rating_count, 0);
