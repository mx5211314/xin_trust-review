-- chat image support: add message.image_key (idempotent)
USE dianping;

SET @e10 := (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='message' AND COLUMN_NAME='image_key');
SET @d10 := IF(@e10=0, 'ALTER TABLE `message` ADD COLUMN image_key VARCHAR(255) NULL AFTER `text`', 'SELECT 1');
PREPARE s10 FROM @d10; EXECUTE s10; DEALLOCATE PREPARE s10;

-- verify
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA='dianping' AND TABLE_NAME='message'
ORDER BY ORDINAL_POSITION;
