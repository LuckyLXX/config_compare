-- 更新 collect_template 表结构
-- 解决 "Unknown column 'update_by' in 'field list'" 错误

USE config_compare;

-- 添加缺失的 update_by 字段
ALTER TABLE collect_template 
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人';

-- 验证表结构
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'config_compare' 
  AND TABLE_NAME = 'collect_template'
ORDER BY ORDINAL_POSITION;
