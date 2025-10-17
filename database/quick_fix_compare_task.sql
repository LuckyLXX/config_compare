-- 快速修复比对任务表
-- 只添加缺失的update_by字段

-- 为compare_task表添加update_by字段
ALTER TABLE compare_task 
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' AFTER create_by;

-- 验证修复结果
DESCRIBE compare_task;

-- 可选：为现有记录设置默认值
UPDATE compare_task SET update_by = 'system' WHERE update_by IS NULL;












