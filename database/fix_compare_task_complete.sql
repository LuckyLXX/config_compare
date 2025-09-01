-- 完整修复比对任务表结构
-- 添加缺失的字段以匹配CompareTask实体类

-- 1. 为compare_task表添加缺失的字段
ALTER TABLE compare_task 
ADD COLUMN server_type_id BIGINT NOT NULL COMMENT '服务器类型ID' AFTER system_id,
ADD COLUMN target_server_ids TEXT COMMENT '目标服务器ID列表，逗号分隔' AFTER baseline_id;

-- 2. 验证compare_task表结构
DESCRIBE compare_task;

-- 3. 为现有记录设置默认值（如果需要）
-- 注意：server_type_id是NOT NULL，需要设置一个有效值
-- 这里假设使用system_id作为默认值，实际使用时需要根据业务逻辑调整
UPDATE compare_task SET server_type_id = system_id WHERE server_type_id IS NULL;

-- 4. 验证修复结果
SELECT COUNT(*) as total_records FROM compare_task;
SELECT server_type_id, COUNT(*) as count FROM compare_task GROUP BY server_type_id;


