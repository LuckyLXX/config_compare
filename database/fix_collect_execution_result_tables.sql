-- 修复collect_execution和collect_result表结构，添加缺失的审计字段
-- 执行此脚本前请确保已连接到正确的数据库
-- 执行时间：2025-08-27

USE config_compare;

-- 1. 修复 collect_execution 表：添加审计字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_execution' 
     AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE collect_execution ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER error_message;',
    'SELECT ''collect_execution.create_time字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_execution' 
     AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE collect_execution ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time;',
    'SELECT ''collect_execution.update_time字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_execution' 
     AND COLUMN_NAME = 'create_by') = 0,
    'ALTER TABLE collect_execution ADD COLUMN create_by VARCHAR(100) DEFAULT ''system'' COMMENT ''创建人'' AFTER update_time;',
    'SELECT ''collect_execution.create_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_execution' 
     AND COLUMN_NAME = 'update_by') = 0,
    'ALTER TABLE collect_execution ADD COLUMN update_by VARCHAR(100) DEFAULT ''system'' COMMENT ''更新人'' AFTER create_by;',
    'SELECT ''collect_execution.update_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 修复 collect_result 表：添加审计字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_result' 
     AND COLUMN_NAME = 'create_time') = 0,
    'ALTER TABLE collect_result ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER retry_count;',
    'SELECT ''collect_result.create_time字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_result' 
     AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE collect_result ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time;',
    'SELECT ''collect_result.update_time字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_result' 
     AND COLUMN_NAME = 'create_by') = 0,
    'ALTER TABLE collect_result ADD COLUMN create_by VARCHAR(100) DEFAULT ''system'' COMMENT ''创建人'' AFTER update_time;',
    'SELECT ''collect_result.create_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_result' 
     AND COLUMN_NAME = 'update_by') = 0,
    'ALTER TABLE collect_result ADD COLUMN update_by VARCHAR(100) DEFAULT ''system'' COMMENT ''更新人'' AFTER create_by;',
    'SELECT ''collect_result.update_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 验证修复结果
SELECT 'collect_execution表字段:' as info;
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'config_compare' 
  AND TABLE_NAME = 'collect_execution'
  AND COLUMN_NAME IN ('create_time', 'update_time', 'create_by', 'update_by')
ORDER BY ORDINAL_POSITION;

SELECT 'collect_result表字段:' as info;
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'config_compare' 
  AND TABLE_NAME = 'collect_result'
  AND COLUMN_NAME IN ('create_time', 'update_time', 'create_by', 'update_by')
ORDER BY ORDINAL_POSITION;

-- 显示修复完成信息
SELECT '数据库表结构修复完成！已为collect_execution和collect_result表添加审计字段。' as message;
