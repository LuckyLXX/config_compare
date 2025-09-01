-- 初始化测试数据脚本
-- 执行前请先运行 schema.sql 创建表结构

USE config_compare;

-- ===================================
-- 测试数据插入
-- ===================================

-- 插入测试系统信息
INSERT INTO sys_system_info (system_name, system_desc, env_type, owner, contact, create_by) VALUES
('交易中心系统', '核心交易系统，支持联机、批量和内管功能', 'PROD', '交易中心团队', 'trading-team@company.com', 'admin'),
('风控系统', '实时风险控制和监控系统', 'PROD', '风控团队', 'risk-team@company.com', 'admin'),
('清算系统', '交易清算和对账系统', 'PROD', '清算团队', 'clearing-team@company.com', 'admin');

-- 插入测试服务器实例（以交易中心系统为例）
INSERT INTO server_instance (system_id, server_type_id, instance_name, server_ip, ssh_port, username, password, server_role, description, create_by) VALUES
-- 交易中心系统 - 应用服务器
(1, 1, '交易中心-联机服务器-01', '10.99.121.1', 22, 'appuser', 'encrypted_password_1', 'MASTER', '联机主服务器', 'admin'),
(1, 1, '交易中心-联机服务器-02', '10.99.121.2', 22, 'appuser', 'encrypted_password_2', 'SLAVE', '联机备服务器', 'admin'),

-- 交易中心系统 - 批量服务器
(1, 3, '交易中心-批量服务器-01', '10.99.122.1', 22, 'batchuser', 'encrypted_password_3', 'MASTER', '批量主服务器', 'admin'),
(1, 3, '交易中心-批量服务器-02', '10.99.122.2', 22, 'batchuser', 'encrypted_password_4', 'SLAVE', '批量备服务器', 'admin'),

-- 交易中心系统 - 内管服务器
(1, 2, '交易中心-内管服务器-01', '10.99.123.1', 22, 'adminuser', 'encrypted_password_5', 'MASTER', '内管主服务器', 'admin'),
(1, 2, '交易中心-内管服务器-02', '10.99.123.2', 22, 'adminuser', 'encrypted_password_6', 'SLAVE', '内管备服务器', 'admin'),

-- 交易中心系统 - Apollo配置中心
(1, 6, '交易中心-Apollo配置中心', NULL, NULL, NULL, NULL, 'MASTER', 'Apollo配置中心服务', 'admin');

-- 更新Apollo相关配置
UPDATE server_instance SET 
    apollo_server_url = 'http://apollo.trading.com:8080',
    apollo_app_id = 'trading-center',
    apollo_cluster = 'default',
    apollo_env = 'PROD',
    apollo_namespaces = 'application,database,redis,mq,risk-control',
    apollo_token = 'apollo_access_token_encrypted'
WHERE id = 7;

-- 插入基线配置示例
INSERT INTO config_baseline (system_id, server_type_id, category_id, baseline_name, baseline_version, file_name, config_content, config_hash, is_default, status, description, source_type, create_by) VALUES
-- 交易中心 - 应用服务器 - 应用配置
(1, 1, 1, '交易中心应用配置基线', 'v1.0.0', 'application.properties', 
'# 交易中心应用配置
server.port=8080
spring.application.name=trading-center
spring.datasource.url=jdbc:mysql://10.99.120.100:3306/trading
spring.datasource.username=trading_user
spring.datasource.password=encrypted_password
spring.redis.host=10.99.120.101
spring.redis.port=6379
trading.max.amount=1000000
trading.timeout=30000', 
'a1b2c3d4e5f6', 1, 1, '交易中心生产环境应用配置基线', 'MANUAL', 'admin'),

-- 交易中心 - 应用服务器 - JVM配置
(1, 1, 2, '交易中心JVM配置基线', 'v1.0.0', 'jvm_args.txt',
'-Xms4g
-Xmx8g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+PrintGCDetails
-XX:+PrintGCTimeStamps
-Djava.awt.headless=true
-Dfile.encoding=UTF-8
-Duser.timezone=Asia/Shanghai',
'b2c3d4e5f6g7', 1, 1, '交易中心生产环境JVM配置基线', 'MANUAL', 'admin'),

-- 交易中心 - Apollo配置中心 - Apollo配置
(1, 6, 6, '交易中心Apollo配置基线', 'v1.0.0', 'apollo_configs.json',
'{
  "application": {
    "timeout": "30000",
    "retry.count": "3",
    "circuit.breaker.enabled": "true"
  },
  "database": {
    "pool.min.size": "10",
    "pool.max.size": "50",
    "connection.timeout": "5000"
  },
  "redis": {
    "pool.max.total": "100",
    "pool.max.idle": "20",
    "timeout": "2000"
  }
}',
'c3d4e5f6g7h8', 1, 1, '交易中心Apollo配置基线', 'MANUAL', 'admin');

-- 插入采集模板示例
INSERT INTO collect_template (template_name, template_type, template_content, applicable_server_types, description, create_by) VALUES
-- 应用服务器配置采集模板
('应用服务器配置采集模板', 'MULTI_TYPE', 
'{
  "templateName": "应用服务器配置采集",
  "templateType": "MULTI_TYPE",
  "applicableServerTypes": ["APP_SERVER"],
  "collectItems": [
    {
      "itemName": "应用配置文件",
      "itemType": "FILE",
      "remotePath": "/opt/app/config/application.properties",
      "localPath": "./download/",
      "timeout": 60
    },
    {
      "itemName": "JVM运行参数",
      "itemType": "COMMAND",
      "command": "ps aux | grep java | head -1",
      "timeout": 30,
      "retryCount": 2
    },
    {
      "itemName": "端口监听情况",
      "itemType": "COMMAND",
      "command": "netstat -tuln | grep :8080",
      "timeout": 15
    }
  ],
  "parallelExecution": true,
  "maxConcurrency": 3
}',
'1', '应用服务器标准配置采集模板', 'admin'),

-- Apollo配置采集模板
('Apollo配置采集模板', 'APOLLO',
'{
  "templateName": "Apollo配置采集",
  "templateType": "APOLLO",
  "apolloConfig": {
    "serverUrl": "http://apollo.trading.com:8080",
    "appId": "trading-center",
    "cluster": "default",
    "env": "PROD",
    "namespaces": [
      "application",
      "database",
      "redis",
      "mq"
    ],
    "token": "${apollo.access.token}"
  },
  "collectItems": [
    {
      "itemName": "Apollo全量配置",
      "itemType": "APOLLO",
      "endpoint": "/openapi/v1/envs/{env}/apps/{appId}/clusters/{cluster}/namespaces/{namespace}/items",
      "method": "GET",
      "headers": {
        "Authorization": "token {token}"
      },
      "timeout": 60
    }
  ]
}',
'6', 'Apollo配置中心标准配置采集模板', 'admin');

-- 插入默认比对规则配置
INSERT INTO collect_config_params (config_group, param_key, param_value, param_type, description) VALUES
-- 比对规则配置
('COMPARE_RULES', 'ignore.patterns', '["^#.*", "^\\\\s*$", ".*timestamp.*"]', 'JSON', '忽略模式列表'),
('COMPARE_RULES', 'normalize.whitespace', 'true', 'BOOLEAN', '是否标准化空白字符'),
('COMPARE_RULES', 'ignore.case', 'false', 'BOOLEAN', '是否忽略大小写'),
('COMPARE_RULES', 'ignore.keys', '["lastModified", "createTime", "timestamp"]', 'JSON', '忽略的键列表'),

-- Apollo配置
('APOLLO', 'default.timeout', '60000', 'INT', 'Apollo默认超时时间（毫秒）'),
('APOLLO', 'max.retry.count', '3', 'INT', 'Apollo最大重试次数'),
('APOLLO', 'connection.pool.size', '10', 'INT', 'Apollo连接池大小'),

-- 系统配置
('SYSTEM', 'max.collect.concurrency', '10', 'INT', '最大采集并发数'),
('SYSTEM', 'default.timeout', '300', 'INT', '默认超时时间（秒）'),
('SYSTEM', 'temp.file.retention.days', '7', 'INT', '临时文件保留天数');