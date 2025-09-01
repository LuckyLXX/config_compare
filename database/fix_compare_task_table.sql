-- 修复比对任务表结构
-- 添加缺失的字段以匹配BaseEntity

-- 1. 为compare_task表添加缺失的字段
-- 注意：create_by字段已经存在，只需要添加update_by字段
ALTER TABLE compare_task 
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' AFTER create_by;

-- 2. 验证compare_task表结构
DESCRIBE compare_task;

-- 3. 为其他相关表也添加缺失的字段（如果需要）
-- 比对执行记录表
ALTER TABLE compare_execution 
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER duration_ms,
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
ADD COLUMN create_by VARCHAR(100) COMMENT '创建人' AFTER update_time,
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' AFTER create_by;

-- 比对结果表
ALTER TABLE compare_result 
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER duration_ms,
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
ADD COLUMN create_by VARCHAR(100) COMMENT '创建人' AFTER update_time,
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' AFTER create_by;

-- 差异详情表
ALTER TABLE diff_detail 
ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER suggest_action,
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
ADD COLUMN create_by VARCHAR(100) COMMENT '创建人' AFTER update_time,
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' AFTER create_by;

-- 4. 验证所有表结构
SHOW TABLES LIKE 'compare_%';
DESCRIBE compare_execution;
DESCRIBE compare_result;
DESCRIBE diff_detail;

-- 5. 如果需要，可以更新现有记录的字段值
-- UPDATE compare_task SET update_by = 'system' WHERE update_by IS NULL;
-- UPDATE compare_execution SET create_by = 'system', update_by = 'system' WHERE create_by IS NULL;
-- UPDATE compare_result SET create_by = 'system', update_by = 'system' WHERE create_by IS NULL;
-- UPDATE diff_detail SET create_by = 'system', update_by = 'system' WHERE create_by IS NULL;
