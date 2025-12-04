-- 插入测试差异详情数据来演示Beyond Compare风格的差异分析功能

INSERT INTO compare_diff_detail (
    result_id, diff_type, diff_key, diff_level, diff_category, 
    description, baseline_value, current_value, suggest_action, diff_path,
    create_time, update_time
) VALUES 
(46, 'MODIFY', '修改第5行', 'MEDIUM', '配置修改', 
 '修改配置项：第5行，原值：server.port=8080，新值：server.port=8081', 
 'server.port=8080', 'server.port=8081', '确认端口修改是否正确', 'line_5',
 NOW(), NOW()),

(46, 'ADD', '新增第12行', 'MEDIUM', '配置新增', 
 '新增配置项：第12行 = spring.datasource.hikari.maximum-pool-size=20', 
 '', 'spring.datasource.hikari.maximum-pool-size=20', '确认新增配置是否正确', 'line_12',
 NOW(), NOW()),

(46, 'DELETE', '删除第8行', 'MEDIUM', '配置缺失', 
 '删除配置项：第8行（原值：logging.level.com.example=DEBUG）', 
 'logging.level.com.example=DEBUG', '', '确认是否需要保留该配置', 'line_8',
 NOW(), NOW()),

(46, 'MODIFY', '修改第15行', 'MEDIUM', '配置修改', 
 '修改配置项：第15行，原值：spring.jpa.hibernate.ddl-auto=create-drop，新值：spring.jpa.hibernate.ddl-auto=update', 
 'spring.jpa.hibernate.ddl-auto=create-drop', 'spring.jpa.hibernate.ddl-auto=update', '确认数据库DDL策略修改是否正确', 'line_15',
 NOW(), NOW()),

(46, 'MODIFY', '修改第20行', 'MEDIUM', '配置修改', 
 '修改配置项：第20行，原值：management.endpoints.web.exposure.include=health,info，新值：management.endpoints.web.exposure.include=*', 
 'management.endpoints.web.exposure.include=health,info', 'management.endpoints.web.exposure.include=*', '确认监控端点暴露范围修改是否安全', 'line_20',
 NOW(), NOW());
