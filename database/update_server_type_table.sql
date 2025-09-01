-- 更新server_type表结构，添加缺失的字段
-- 执行时间: 2025-01-26

USE config_compare;

-- 添加update_time字段
ALTER TABLE server_type 
ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' 
AFTER create_time;

-- 添加update_by字段  
ALTER TABLE server_type 
ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' 
AFTER update_time;

-- 验证表结构
DESC server_type;

-- 显示现有数据
SELECT * FROM server_type;
