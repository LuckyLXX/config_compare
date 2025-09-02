-- ===================================
-- 修复比对相关表结构
-- 添加缺失的审计字段
-- ===================================

-- 1. 修复 compare_execution 表
ALTER TABLE compare_execution 
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER duration_ms,
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
ADD COLUMN execute_by VARCHAR(100) COMMENT '执行人' AFTER update_time,
ADD COLUMN error_message TEXT COMMENT '错误信息' AFTER execute_by;

-- 为 compare_execution 表添加索引
ALTER TABLE compare_execution 
ADD INDEX idx_create_time (create_time),
ADD INDEX idx_update_time (update_time);

-- 2. 修复 compare_result 表
ALTER TABLE compare_result 
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER duration_ms,
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
ADD COLUMN error_message TEXT COMMENT '错误信息' AFTER update_time;

-- 为 compare_result 表添加索引
ALTER TABLE compare_result 
ADD INDEX idx_create_time (create_time),
ADD INDEX idx_update_time (update_time);

-- 3. 更新现有数据
-- 更新 compare_execution 表的创建时间和更新时间
UPDATE compare_execution 
SET create_time = start_time, 
    update_time = start_time 
WHERE create_time IS NULL;

-- 更新 compare_result 表的创建时间和更新时间
UPDATE compare_result 
SET create_time = execute_time, 
    update_time = execute_time 
WHERE create_time IS NULL;

-- 4. 显示表结构
SELECT 'compare_execution 表结构:' as info;
DESCRIBE compare_execution;

SELECT 'compare_result 表结构:' as info;
DESCRIBE compare_result;
