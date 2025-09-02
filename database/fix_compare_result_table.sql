-- ===================================
-- 修复比对结果表结构
-- 添加缺失的审计字段
-- ===================================

-- 为 compare_result 表添加缺失字段
ALTER TABLE compare_result 
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER duration_ms,
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
ADD COLUMN error_message TEXT COMMENT '错误信息' AFTER update_time;

-- 添加索引
ALTER TABLE compare_result 
ADD INDEX idx_create_time (create_time),
ADD INDEX idx_update_time (update_time);

-- 更新现有数据的创建时间和更新时间
UPDATE compare_result 
SET create_time = execute_time, 
    update_time = execute_time 
WHERE create_time IS NULL;

-- 显示表结构
DESCRIBE compare_result;
