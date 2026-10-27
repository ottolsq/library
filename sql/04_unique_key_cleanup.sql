-- 04_unique_key_cleanup.sql 兼容旧库并清理历史逻辑删除记录占用的唯一键
--
-- 背景：
--   1. 早期 user 表没有 is_deleted 列，当前代码使用 @TableLogic 后必须先补列；
--   2. 早期逻辑删除用户/图书时未改写唯一键，导致同名账号或同 ISBN 图书无法重新创建。
--
-- 本脚本可重复执行：已有列会跳过，已改写的记录会再次生成新的唯一值。
USE book_library;

-- ========== 1. 补齐 user.is_deleted ==========
SET @user_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'is_deleted'
);
SET @user_ddl = IF(@user_col_exists = 0,
    'ALTER TABLE `user` ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删，1已删'' AFTER `status`',
    'SELECT 1');
PREPARE user_stmt FROM @user_ddl;
EXECUTE user_stmt;
DEALLOCATE PREPARE user_stmt;

-- ========== 2. 补齐 book.is_deleted（更早的库可能也没有） ==========
SET @book_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'book'
      AND COLUMN_NAME = 'is_deleted'
);
SET @book_ddl = IF(@book_col_exists = 0,
    'ALTER TABLE `book` ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除 0未删，1已删'' AFTER `available_num`',
    'SELECT 1');
PREPARE book_stmt FROM @book_ddl;
EXECUTE book_stmt;
DEALLOCATE PREPARE book_stmt;

-- ========== 3. 改写已逻辑删除记录的唯一键 ==========
UPDATE `user`
SET `username` = LEFT(CONCAT('deleted_', `user_id`, '_', REPLACE(UUID(), '-', '')), 50)
WHERE `is_deleted` = 1;

UPDATE `book`
SET `isbn` = LEFT(CONCAT('del', `book_id`, '_', REPLACE(UUID(), '-', '')), 30)
WHERE `is_deleted` = 1;
