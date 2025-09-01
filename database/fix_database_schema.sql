-- 修复数据库表结构，添加缺失的字段
-- 执行此脚本前请确保已连接到正确的数据库

USE config_compare;

-- 修复 collect_template 表
ALTER TABLE collect_template 
ADD COLUMN IF NOT EXISTS update_by VARCHAR(100) COMMENT '更新人';

-- 检查其他可能缺失 update_by 字段的表
-- 如果这些表也报同样的错误，请取消注释相应行

-- ALTER TABLE collect_task 
-- ADD COLUMN IF NOT EXISTS update_by VARCHAR(100) COMMENT '更新人';

-- ALTER TABLE compare_task 
-- ADD COLUMN IF NOT EXISTS update_by VARCHAR(100) COMMENT '更新人';

-- 验证修复结果
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'config_compare' 
  AND TABLE_NAME = 'collect_template'
  AND COLUMN_NAME IN ('create_by', 'update_by')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- 显示修复完成信息
SELECT 'collect_template 表结构修复完成!' as message;
