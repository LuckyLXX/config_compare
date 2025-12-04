-- UAT生产环境配置比对系统MySQL完整初始化脚本
-- 创建时间: 2025-10-24
-- 版本: v1.0.0
-- 适用于MySQL 5.7+ / 8.0+
-- 内网部署版本

-- ===================================
-- 1. 创建数据库
-- ===================================

-- 如果不存在则创建数据库
CREATE DATABASE IF NOT EXISTS config_compare DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE config_compare;

-- ===================================
-- 2. 基础管理表
-- ===================================

-- 系统信息表
CREATE TABLE IF NOT EXISTS sys_system_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_name VARCHAR(100) NOT NULL UNIQUE COMMENT '系统名称',
    system_desc VARCHAR(500) COMMENT '系统描述',
    env_type VARCHAR(20) NOT NULL COMMENT '环境类型：UAT/PROD',
    owner VARCHAR(100) COMMENT '系统负责人',
    contact VARCHAR(200) COMMENT '联系方式',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_system_name (system_name),
    INDEX idx_env_type (env_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统信息表';

-- 服务器类型表
CREATE TABLE IF NOT EXISTS server_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_name VARCHAR(100) NOT NULL COMMENT '服务器类型名称',
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '服务器类型编码',
    description VARCHAR(500) COMMENT '类型描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_type_code (type_code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器类型表';

-- 服务器实例表
CREATE TABLE IF NOT EXISTS server_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_id BIGINT NOT NULL COMMENT '系统ID',
    server_type_id BIGINT NOT NULL COMMENT '服务器类型ID',
    instance_name VARCHAR(200) NOT NULL COMMENT '实例名称',
    server_ip VARCHAR(50) COMMENT '服务器IP（SSH/SFTP类型使用）',
    ssh_port INT DEFAULT 22 COMMENT 'SSH端口',
    username VARCHAR(100) COMMENT '连接用户名',
    password VARCHAR(500) COMMENT '连接密码（加密存储）',
    server_role VARCHAR(50) COMMENT '服务器角色：MASTER/SLAVE/BACKUP',
    
    -- Apollo配置相关字段
    apollo_server_url VARCHAR(200) COMMENT 'Apollo服务器地址',
    apollo_app_id VARCHAR(100) COMMENT 'Apollo应用标识',
    apollo_cluster VARCHAR(100) COMMENT 'Apollo集群名称',
    apollo_env VARCHAR(20) COMMENT 'Apollo环境',
    apollo_namespaces TEXT COMMENT 'Apollo命名空间列表，逗号分隔',
    apollo_token VARCHAR(500) COMMENT 'Apollo访问令牌',
    
    -- 通用扩展字段
    custom_config LONGTEXT COMMENT '自定义配置参数JSON',
    
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0异常',
    last_connect_time DATETIME COMMENT '最后连接时间',
    connect_status TINYINT DEFAULT 0 COMMENT '连接状态：1正常 0异常',
    description VARCHAR(500) COMMENT '实例描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_system_id (system_id),
    INDEX idx_server_type (server_type_id),
    INDEX idx_server_ip (server_ip),
    INDEX idx_apollo_app (apollo_app_id),
    INDEX idx_status (status),
    UNIQUE KEY uk_system_type_name (system_id, server_type_id, instance_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='服务器实例表';

-- 配置分类表
CREATE TABLE IF NOT EXISTS config_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    applicable_types TEXT COMMENT '适用服务器类型ID列表，逗号分隔',
    description VARCHAR(500) COMMENT '分类描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_category_code (category_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置分类表';

-- ===================================
-- 3. 基线管理表
-- ===================================

-- 配置基线表
CREATE TABLE IF NOT EXISTS config_baseline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_id BIGINT NOT NULL COMMENT '系统ID',
    server_type_id BIGINT NOT NULL COMMENT '服务器类型ID',
    category_id BIGINT NOT NULL COMMENT '配置分类ID',
    baseline_name VARCHAR(200) NOT NULL COMMENT '基线名称',
    baseline_version VARCHAR(50) NOT NULL COMMENT '基线版本',
    file_name VARCHAR(200) COMMENT '原始文件名',
    config_content LONGTEXT NOT NULL COMMENT '配置内容',
    config_hash VARCHAR(64) COMMENT '配置内容哈希值',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认版本：1是 0否',
    status TINYINT DEFAULT 0 COMMENT '状态：0草稿 1生效 2归档',
    description TEXT COMMENT '基线描述',
    source_type VARCHAR(20) DEFAULT 'MANUAL' COMMENT '来源类型：MANUAL/IMPORT/COPY',
    source_baseline_id BIGINT COMMENT '来源基线ID（复制时使用）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_system_id (system_id),
    INDEX idx_server_type (server_type_id),
    INDEX idx_category (category_id),
    INDEX idx_version (baseline_version),
    INDEX idx_status (status),
    INDEX idx_default (is_default),
    INDEX idx_config_hash (config_hash),
    UNIQUE KEY uk_system_type_category_default (system_id, server_type_id, category_id, is_default)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置基线表';

-- 基线版本切换日志表
CREATE TABLE IF NOT EXISTS baseline_version_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_id BIGINT NOT NULL COMMENT '系统ID',
    server_type_id BIGINT NOT NULL COMMENT '服务器类型ID',
    category_id BIGINT NOT NULL COMMENT '配置分类ID',
    old_baseline_id BIGINT COMMENT '原默认基线ID',
    new_baseline_id BIGINT NOT NULL COMMENT '新默认基线ID',
    old_version VARCHAR(50) COMMENT '原版本号',
    new_version VARCHAR(50) NOT NULL COMMENT '新版本号',
    switch_reason TEXT COMMENT '切换原因',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型：SWITCH/ROLLBACK',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '操作人',
    INDEX idx_system_id (system_id),
    INDEX idx_server_type (server_type_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='基线版本切换日志表';

-- ===================================
-- 4. 采集相关表
-- ===================================

-- 采集类型扩展表
CREATE TABLE IF NOT EXISTS collect_type_extension (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    type_category VARCHAR(50) NOT NULL COMMENT '类型分类：BASIC/EXTENDED/CUSTOM',
    handler_class VARCHAR(200) NOT NULL COMMENT '处理器类名',
    config_schema LONGTEXT COMMENT '配置参数架构JSON Schema',
    description TEXT COMMENT '类型描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_type_code (type_code),
    INDEX idx_type_category (type_category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集类型扩展表';

-- 采集参数配置表
CREATE TABLE IF NOT EXISTS collect_config_params (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    config_group VARCHAR(100) NOT NULL COMMENT '配置组：APOLLO/DATABASE/CUSTOM',
    param_key VARCHAR(100) NOT NULL COMMENT '参数键',
    param_value VARCHAR(500) COMMENT '参数值',
    param_type VARCHAR(20) DEFAULT 'STRING' COMMENT '参数类型：STRING/INT/BOOLEAN/JSON',
    is_encrypted TINYINT DEFAULT 0 COMMENT '是否加密：1是 0否',
    description VARCHAR(200) COMMENT '参数描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_group_key (config_group, param_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集参数配置表';

-- 采集模板表
CREATE TABLE IF NOT EXISTS collect_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型：SERVER_CONFIG/FILE_CONFIG/API_CONFIG/APOLLO/MULTI_TYPE',
    template_content LONGTEXT NOT NULL COMMENT '模板内容JSON',
    applicable_server_types TEXT COMMENT '适用服务器类型列表',
    config_params LONGTEXT COMMENT '扩展配置参数JSON',
    description TEXT COMMENT '模板描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_template_type (template_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集模板表';

-- 采集任务表
CREATE TABLE IF NOT EXISTS collect_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    system_id BIGINT NOT NULL COMMENT '系统ID',
    server_type_ids TEXT COMMENT '服务器类型ID列表，逗号分隔',
    server_instance_ids TEXT COMMENT '服务器实例ID列表，逗号分隔',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    execute_type TINYINT NOT NULL COMMENT '执行类型：1立即执行 2定时执行',
    max_concurrency INT DEFAULT 5 COMMENT '最大并发数',
    timeout_seconds INT DEFAULT 300 COMMENT '超时时间（秒）',
    retry_count INT DEFAULT 2 COMMENT '重试次数',
    status TINYINT DEFAULT 1 COMMENT '任务状态：1启用 0禁用',
    last_execute_time DATETIME COMMENT '最后执行时间',
    next_execute_time DATETIME COMMENT '下次执行时间',
    description TEXT COMMENT '任务描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_system_id (system_id),
    INDEX idx_status (status),
    INDEX idx_next_execute_time (next_execute_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集任务表';

-- 采集执行记录表
CREATE TABLE IF NOT EXISTS collect_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execute_id VARCHAR(50) NOT NULL COMMENT '执行ID',
    execute_status TINYINT NOT NULL COMMENT '执行状态：1成功 2部分成功 3失败 4运行中',
    total_servers INT NOT NULL COMMENT '总服务器数',
    success_servers INT DEFAULT 0 COMMENT '成功服务器数',
    failed_servers INT DEFAULT 0 COMMENT '失败服务器数',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    error_message TEXT COMMENT '错误信息',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    update_by VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_start_time (start_time),
    INDEX idx_execute_status (execute_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集执行记录表';

-- 采集结果表
CREATE TABLE IF NOT EXISTS collect_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execute_id VARCHAR(50) NOT NULL COMMENT '执行ID',
    server_instance_id BIGINT NOT NULL COMMENT '服务器实例ID',
    collect_item_name VARCHAR(200) NOT NULL COMMENT '采集项名称',
    collect_type VARCHAR(50) NOT NULL COMMENT '采集类型：COMMAND/FILE/API/APOLLO',
    collect_content LONGTEXT COMMENT '采集内容',
    file_path VARCHAR(500) COMMENT '文件路径（文件采集时使用）',
    api_endpoint VARCHAR(500) COMMENT 'API端点（API采集时使用）',
    namespace VARCHAR(100) COMMENT '命名空间（Apollo采集时使用）',
    collect_status TINYINT NOT NULL COMMENT '采集状态：1成功 0失败',
    error_message TEXT COMMENT '错误信息',
    execute_time DATETIME NOT NULL COMMENT '执行时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_server_instance (server_instance_id),
    INDEX idx_execute_time (execute_time),
    INDEX idx_collect_type (collect_type),
    INDEX idx_collect_status (collect_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采集结果表';

-- ===================================
-- 5. 比对相关表
-- ===================================

-- 比对任务表
CREATE TABLE IF NOT EXISTS compare_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    system_id BIGINT NOT NULL COMMENT '系统ID',
    server_type_id BIGINT NOT NULL COMMENT '服务器类型ID',
    category_id BIGINT NOT NULL COMMENT '配置分类ID',
    baseline_id BIGINT COMMENT '指定基线ID（为空则使用默认基线）',
    target_server_ids TEXT COMMENT '目标服务器ID列表，逗号分隔',
    collect_task_id BIGINT COMMENT '关联采集任务ID',
    compare_rules LONGTEXT COMMENT '比对规则JSON',
    execute_type TINYINT NOT NULL COMMENT '执行类型：1立即执行 2定时执行 3触发执行',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    auto_execute TINYINT DEFAULT 0 COMMENT '是否自动执行：1是 0否',
    status TINYINT DEFAULT 1 COMMENT '任务状态：1启用 0禁用',
    last_execute_time DATETIME COMMENT '最后执行时间',
    next_execute_time DATETIME COMMENT '下次执行时间',
    description TEXT COMMENT '任务描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_system_id (system_id),
    INDEX idx_server_type (server_type_id),
    INDEX idx_category (category_id),
    INDEX idx_status (status),
    INDEX idx_next_execute_time (next_execute_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比对任务表';

-- 比对执行记录表
CREATE TABLE IF NOT EXISTS compare_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '比对任务ID',
    execute_id VARCHAR(50) NOT NULL COMMENT '执行ID',
    baseline_id BIGINT NOT NULL COMMENT '使用的基线ID',
    baseline_version VARCHAR(50) NOT NULL COMMENT '基线版本',
    execute_status TINYINT NOT NULL COMMENT '执行状态：1成功 2部分成功 3失败',
    total_servers INT NOT NULL COMMENT '总服务器数',
    consistent_servers INT DEFAULT 0 COMMENT '一致服务器数',
    inconsistent_servers INT DEFAULT 0 COMMENT '不一致服务器数',
    failed_servers INT DEFAULT 0 COMMENT '比对失败服务器数',
    overall_score DECIMAL(5,2) COMMENT '整体一致性评分',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    execute_by VARCHAR(100) COMMENT '执行人',
    error_message TEXT COMMENT '错误信息',
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_start_time (start_time),
    INDEX idx_execute_status (execute_status),
    INDEX idx_create_time (create_time),
    INDEX idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比对执行记录表';

-- 比对结果表
CREATE TABLE IF NOT EXISTS compare_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '比对任务ID',
    execute_id VARCHAR(50) NOT NULL COMMENT '执行ID',
    baseline_id BIGINT NOT NULL COMMENT '基线ID',
    server_instance_id BIGINT NOT NULL COMMENT '服务器实例ID',
    collect_result_id BIGINT NOT NULL COMMENT '采集结果ID',
    compare_status TINYINT NOT NULL COMMENT '比对状态：1一致 0不一致 -1比对失败',
    consistency_score DECIMAL(5,2) COMMENT '一致性评分',
    diff_count INT DEFAULT 0 COMMENT '差异数量',
    high_diff_count INT DEFAULT 0 COMMENT '高级别差异数量',
    medium_diff_count INT DEFAULT 0 COMMENT '中级别差异数量',
    low_diff_count INT DEFAULT 0 COMMENT '低级别差异数量',
    diff_summary LONGTEXT COMMENT '差异摘要JSON',
    execute_time DATETIME NOT NULL COMMENT '执行时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    error_message TEXT COMMENT '错误信息',
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_server_instance (server_instance_id),
    INDEX idx_execute_time (execute_time),
    INDEX idx_compare_status (compare_status),
    INDEX idx_create_time (create_time),
    INDEX idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比对结果表';

-- 差异详情表（兼容两种命名）
CREATE TABLE IF NOT EXISTS diff_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    result_id BIGINT NOT NULL COMMENT '比对结果ID',
    diff_type VARCHAR(50) NOT NULL COMMENT '差异类型：ADD/DELETE/MODIFY',
    diff_path VARCHAR(500) COMMENT '差异路径',
    diff_key VARCHAR(200) COMMENT '差异键名',
    baseline_value LONGTEXT COMMENT '基线值',
    current_value LONGTEXT COMMENT '当前值',
    diff_level VARCHAR(20) NOT NULL COMMENT '差异级别：HIGH/MEDIUM/LOW',
    diff_category VARCHAR(100) COMMENT '差异分类',
    suggest_action VARCHAR(200) COMMENT '建议操作',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    update_by VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    INDEX idx_result_id (result_id),
    INDEX idx_diff_level (diff_level),
    INDEX idx_diff_type (diff_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='差异详情表';

-- 比对差异详情表（更完整的版本）
CREATE TABLE IF NOT EXISTS compare_diff_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    result_id BIGINT NOT NULL COMMENT '比对结果ID',
    diff_type VARCHAR(20) NOT NULL COMMENT '差异类型：ADD/DELETE/MODIFY',
    diff_key VARCHAR(255) NOT NULL COMMENT '差异键',
    diff_level VARCHAR(20) NOT NULL COMMENT '差异级别：HIGH/MEDIUM/LOW',
    diff_category VARCHAR(100) DEFAULT NULL COMMENT '差异分类',
    description TEXT COMMENT '差异描述',
    baseline_value TEXT COMMENT '基线值',
    current_value TEXT COMMENT '当前值',
    suggest_action VARCHAR(500) DEFAULT NULL COMMENT '建议操作',
    diff_path VARCHAR(255) DEFAULT NULL COMMENT '差异路径',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_result_id (result_id),
    INDEX idx_diff_type (diff_type),
    INDEX idx_diff_level (diff_level),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比对差异详情表';

-- ===================================
-- 6. 比对规则表
-- ===================================

-- 比对规则表
CREATE TABLE IF NOT EXISTS compare_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT COMMENT '比对任务ID',
    compare_type VARCHAR(50) COMMENT '比对类型',
    ssh_compare_mode VARCHAR(50) COMMENT 'SSH文本比对方式',
    ignore_lines TEXT COMMENT '忽略行关键词',
    file_type VARCHAR(50) COMMENT '文件类型',
    file_compare_options LONGTEXT COMMENT '文件比对选项（JSON格式）',
    apollo_compare_mode VARCHAR(50) COMMENT 'Apollo比对模式',
    ignore_fields TEXT COMMENT '忽略字段',
    sensitive_fields TEXT COMMENT '敏感字段',
    yaml_compare_options LONGTEXT COMMENT 'YAML比对选项（JSON格式）',
    env_compare_mode VARCHAR(50) COMMENT '环境变量比对方式',
    env_template TEXT COMMENT '环境变量模板',
    custom_rules LONGTEXT COMMENT '自定义规则（JSON格式）',
    diff_threshold DECIMAL(10,2) COMMENT '差异阈值',
    ignore_differences LONGTEXT COMMENT '忽略差异选项（JSON格式）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比对规则表';

-- ===================================
-- 7. 报告相关表
-- ===================================

-- 报告模板表
CREATE TABLE IF NOT EXISTS report_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型',
    template_content LONGTEXT NOT NULL COMMENT '模板内容',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_template_type (template_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告模板表';

-- 报告记录表
CREATE TABLE IF NOT EXISTS report_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    report_name VARCHAR(200) NOT NULL COMMENT '报告名称',
    report_type VARCHAR(50) NOT NULL COMMENT '报告类型',
    system_ids TEXT COMMENT '涉及系统ID列表',
    time_range VARCHAR(100) COMMENT '时间范围',
    report_content LONGTEXT COMMENT '报告内容JSON',
    file_path VARCHAR(500) COMMENT '报告文件路径',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '创建人',
    INDEX idx_report_type (report_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告记录表';

-- ===================================
-- 7. 初始化数据
-- ===================================

-- 插入默认服务器类型
INSERT IGNORE INTO server_type (type_name, type_code, description, create_by) VALUES
('应用服务器', 'APP_SERVER', '运行应用程序的服务器', 'system'),
('内管服务器', 'ADMIN_SERVER', '内部管理和监控的服务器', 'system'),
('批量服务器', 'BATCH_SERVER', '处理批量任务的服务器', 'system'),
('数据库服务器', 'DB_SERVER', '数据库服务器', 'system'),
('网关服务器', 'GATEWAY_SERVER', '网关和代理服务器', 'system'),
('Apollo配置中心', 'APOLLO_CONFIG', 'Apollo配置中心服务', 'system');

-- 插入默认配置分类
INSERT IGNORE INTO config_category (category_name, category_code, applicable_types, description, create_by) VALUES
('应用配置', 'APP_CONFIG', '1,2,3', '微服务应用配置文件', 'system'),
('JVM配置', 'JVM_CONFIG', '1,2,3', 'Java虚拟机运行参数', 'system'),
('系统资源', 'SYSTEM_RESOURCE', '1,2,3,4,5', '系统资源使用情况', 'system'),
('网络配置', 'NETWORK_CONFIG', '1,2,3,4,5', '网络相关配置', 'system'),
('中间件配置', 'MIDDLEWARE_CONFIG', '1,2,3,4', '中间件相关配置', 'system'),
('Apollo配置', 'APOLLO_CONFIG', '6', 'Apollo配置中心的配置项', 'system');

-- 插入默认采集类型
INSERT IGNORE INTO collect_type_extension (type_code, type_name, type_category, handler_class, description, create_by) VALUES
('COMMAND', 'SSH命令执行', 'BASIC', 'com.config.compare.collect.handler.CommandCollectHandler', '通过SSH执行命令采集配置', 'system'),
('FILE', 'SFTP文件下载', 'BASIC', 'com.config.compare.collect.handler.FileCollectHandler', '通过SFTP下载文件采集配置', 'system'),
('API', 'HTTP接口调用', 'BASIC', 'com.config.compare.collect.handler.ApiCollectHandler', '通过HTTP接口调用采集配置', 'system'),
('APOLLO', 'Apollo配置中心', 'EXTENDED', 'com.config.compare.collect.handler.ApolloCollectHandler', '从Apollo配置中心采集配置', 'system'),
('DATABASE', '数据库查询', 'EXTENDED', 'com.config.compare.collect.handler.DatabaseCollectHandler', '通过数据库查询采集配置', 'system');

-- 插入默认比对规则配置
INSERT IGNORE INTO collect_config_params (config_group, param_key, param_value, param_type, description) VALUES
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

-- ===================================
-- 8. 创建数据库用户和授权（内网部署）
-- ===================================

-- 创建应用用户（根据实际需求修改密码）
CREATE USER IF NOT EXISTS 'config_compare_app'@'%' IDENTIFIED BY 'ConfigCompare_App_2024!';
CREATE USER IF NOT EXISTS 'config_compare_app'@'localhost' IDENTIFIED BY 'ConfigCompare_App_2024!';

-- 授权应用用户
GRANT SELECT, INSERT, UPDATE, DELETE ON config_compare.* TO 'config_compare_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON config_compare.* TO 'config_compare_app'@'localhost';

-- 创建只读用户（用于报表查询）
CREATE USER IF NOT EXISTS 'config_compare_readonly'@'%' IDENTIFIED BY 'ConfigCompare_Readonly_2024!';
CREATE USER IF NOT EXISTS 'config_compare_readonly'@'localhost' IDENTIFIED BY 'ConfigCompare_Readonly_2024!';

-- 授权只读用户
GRANT SELECT ON config_compare.* TO 'config_compare_readonly'@'%';
GRANT SELECT ON config_compare.* TO 'config_compare_readonly'@'localhost';

-- 刷新权限
FLUSH PRIVILEGES;

-- ===================================
-- 9. 完成提示
-- ===================================

SELECT 'MySQL数据库初始化完成！' AS message;
SELECT '请根据实际内网环境修改数据库用户密码和访问权限' AS security_note;
SELECT '建议定期备份数据库' AS backup_tip;