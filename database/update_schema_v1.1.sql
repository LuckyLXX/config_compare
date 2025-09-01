-- 数据库Schema更新脚本 v1.1
-- 执行时间：2025-01-25
-- 说明：添加缺失的update_by字段到相关表

USE config_compare;

-- 1. 修复collect_task表：添加update_by字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_task' 
     AND COLUMN_NAME = 'update_by') = 0,
    'ALTER TABLE collect_task ADD COLUMN update_by VARCHAR(100) COMMENT ''更新人'' AFTER create_by;',
    'SELECT ''collect_task.update_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 修复collect_type_extension表：添加update_time和update_by字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_type_extension' 
     AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE collect_type_extension ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time;',
    'SELECT ''collect_type_extension.update_time字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'collect_type_extension' 
     AND COLUMN_NAME = 'update_by') = 0,
    'ALTER TABLE collect_type_extension ADD COLUMN update_by VARCHAR(100) COMMENT ''更新人'' AFTER create_by;',
    'SELECT ''collect_type_extension.update_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 修复report_template表：添加update_time和update_by字段
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'report_template' 
     AND COLUMN_NAME = 'update_time') = 0,
    'ALTER TABLE report_template ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER create_time;',
    'SELECT ''report_template.update_time字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS 
     WHERE TABLE_SCHEMA = DATABASE() 
     AND TABLE_NAME = 'report_template' 
     AND COLUMN_NAME = 'update_by') = 0,
    'ALTER TABLE report_template ADD COLUMN update_by VARCHAR(100) COMMENT ''更新人'' AFTER create_by;',
    'SELECT ''report_template.update_by字段已存在'' AS message;'
));
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 显示更新完成信息
SELECT 'Schema更新完成！已添加缺失的update_by字段到相关表。' AS message;
