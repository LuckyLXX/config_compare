-- 修复 collect_template 表结构
-- 添加缺失的 update_by 字段

USE config_compare;

-- 检查并添加 update_by 字段
ALTER TABLE collect_template 
ADD COLUMN IF NOT EXISTS update_by VARCHAR(100) COMMENT '更新人';

-- 显示修复后的表结构
DESCRIBE collect_template;
