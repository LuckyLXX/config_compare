-- UAT生产环境配置比对系统Oracle数据库设计
-- 创建时间: 2025-01-25
-- 版本: v1.0.0
-- 适配Oracle 19c

-- ===================================
-- 1. 创建表空间和用户
-- ===================================

-- 创建表空间（根据实际情况调整路径和大小）
CREATE TABLESPACE config_compare_data
DATAFILE 'config_compare_data.dbf' SIZE 100M
AUTOEXTEND ON NEXT 10M MAXSIZE UNLIMITED;

-- 创建用户
CREATE USER config_compare IDENTIFIED BY "config_compare_2024"
DEFAULT TABLESPACE config_compare_data
TEMPORARY TABLESPACE temp;

-- 授权
GRANT CONNECT, RESOURCE, DBA TO config_compare;
GRANT UNLIMITED TABLESPACE TO config_compare;

-- ===================================
-- 2. 基础管理表
-- ===================================

-- 系统信息表
CREATE TABLE sys_system_info (
    id NUMBER(20) PRIMARY KEY,
    system_name VARCHAR2(100) NOT NULL UNIQUE,
    system_desc VARCHAR2(500),
    env_type VARCHAR2(20) NOT NULL,
    owner VARCHAR2(100),
    contact VARCHAR2(200),
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE sys_system_info IS '系统信息表';
COMMENT ON COLUMN sys_system_info.id IS '主键ID';
COMMENT ON COLUMN sys_system_info.system_name IS '系统名称';
COMMENT ON COLUMN sys_system_info.system_desc IS '系统描述';
COMMENT ON COLUMN sys_system_info.env_type IS '环境类型：UAT/PROD';
COMMENT ON COLUMN sys_system_info.owner IS '系统负责人';
COMMENT ON COLUMN sys_system_info.contact IS '联系方式';
COMMENT ON COLUMN sys_system_info.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN sys_system_info.create_time IS '创建时间';
COMMENT ON COLUMN sys_system_info.update_time IS '更新时间';
COMMENT ON COLUMN sys_system_info.create_by IS '创建人';
COMMENT ON COLUMN sys_system_info.update_by IS '更新人';

-- 创建序列
CREATE SEQUENCE seq_sys_system_info
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

-- 创建触发器
CREATE OR REPLACE TRIGGER trg_sys_system_info
BEFORE INSERT ON sys_system_info
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_sys_system_info.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

-- 创建索引
CREATE INDEX idx_sys_system_name ON sys_system_info(system_name);
CREATE INDEX idx_sys_env_type ON sys_system_info(env_type);
CREATE INDEX idx_sys_status ON sys_system_info(status);

-- 服务器类型表
CREATE TABLE server_type (
    id NUMBER(20) PRIMARY KEY,
    type_name VARCHAR2(100) NOT NULL,
    type_code VARCHAR2(50) NOT NULL UNIQUE,
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE server_type IS '服务器类型表';
COMMENT ON COLUMN server_type.id IS '主键ID';
COMMENT ON COLUMN server_type.type_name IS '服务器类型名称';
COMMENT ON COLUMN server_type.type_code IS '服务器类型编码';
COMMENT ON COLUMN server_type.description IS '类型描述';
COMMENT ON COLUMN server_type.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN server_type.create_time IS '创建时间';
COMMENT ON COLUMN server_type.update_time IS '更新时间';
COMMENT ON COLUMN server_type.create_by IS '创建人';
COMMENT ON COLUMN server_type.update_by IS '更新人';

CREATE SEQUENCE seq_server_type
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_server_type
BEFORE INSERT ON server_type
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_server_type.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_server_type_code ON server_type(type_code);
CREATE INDEX idx_server_type_status ON server_type(status);

-- 服务器实例表
CREATE TABLE server_instance (
    id NUMBER(20) PRIMARY KEY,
    system_id NUMBER(20) NOT NULL,
    server_type_id NUMBER(20) NOT NULL,
    instance_name VARCHAR2(200) NOT NULL,
    server_ip VARCHAR2(50),
    ssh_port NUMBER(10) DEFAULT 22,
    username VARCHAR2(100),
    password VARCHAR2(500),
    server_role VARCHAR2(50),
    apollo_server_url VARCHAR2(200),
    apollo_app_id VARCHAR2(100),
    apollo_cluster VARCHAR2(100),
    apollo_env VARCHAR2(20),
    apollo_namespaces CLOB,
    apollo_token VARCHAR2(500),
    custom_config CLOB,
    status NUMBER(1) DEFAULT 1,
    last_connect_time TIMESTAMP,
    connect_status NUMBER(1) DEFAULT 0,
    description VARCHAR2(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100),
    CONSTRAINT uk_server_instance UNIQUE (system_id, server_type_id, instance_name)
);

-- 添加表注释
COMMENT ON TABLE server_instance IS '服务器实例表';
COMMENT ON COLUMN server_instance.id IS '主键ID';
COMMENT ON COLUMN server_instance.system_id IS '系统ID';
COMMENT ON COLUMN server_instance.server_type_id IS '服务器类型ID';
COMMENT ON COLUMN server_instance.instance_name IS '实例名称';
COMMENT ON COLUMN server_instance.server_ip IS '服务器IP（SSH/SFTP类型使用）';
COMMENT ON COLUMN server_instance.ssh_port IS 'SSH端口';
COMMENT ON COLUMN server_instance.username IS '连接用户名';
COMMENT ON COLUMN server_instance.password IS '连接密码（加密存储）';
COMMENT ON COLUMN server_instance.server_role IS '服务器角色：MASTER/SLAVE/BACKUP';
COMMENT ON COLUMN server_instance.apollo_server_url IS 'Apollo服务器地址';
COMMENT ON COLUMN server_instance.apollo_app_id IS 'Apollo应用标识';
COMMENT ON COLUMN server_instance.apollo_cluster IS 'Apollo集群名称';
COMMENT ON COLUMN server_instance.apollo_env IS 'Apollo环境';
COMMENT ON COLUMN server_instance.apollo_namespaces IS 'Apollo命名空间列表，逗号分隔';
COMMENT ON COLUMN server_instance.apollo_token IS 'Apollo访问令牌';
COMMENT ON COLUMN server_instance.custom_config IS '自定义配置参数JSON';
COMMENT ON COLUMN server_instance.status IS '状态：1启用 0异常';
COMMENT ON COLUMN server_instance.last_connect_time IS '最后连接时间';
COMMENT ON COLUMN server_instance.connect_status IS '连接状态：1正常 0异常';
COMMENT ON COLUMN server_instance.description IS '实例描述';
COMMENT ON COLUMN server_instance.create_time IS '创建时间';
COMMENT ON COLUMN server_instance.update_time IS '更新时间';
COMMENT ON COLUMN server_instance.create_by IS '创建人';
COMMENT ON COLUMN server_instance.update_by IS '更新人';

CREATE SEQUENCE seq_server_instance
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_server_instance
BEFORE INSERT ON server_instance
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_server_instance.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_server_instance_system_id ON server_instance(system_id);
CREATE INDEX idx_server_instance_type_id ON server_instance(server_type_id);
CREATE INDEX idx_server_instance_ip ON server_instance(server_ip);
CREATE INDEX idx_server_instance_apollo_app ON server_instance(apollo_app_id);
CREATE INDEX idx_server_instance_status ON server_instance(status);

-- 配置分类表
CREATE TABLE config_category (
    id NUMBER(20) PRIMARY KEY,
    category_name VARCHAR2(100) NOT NULL,
    category_code VARCHAR2(50) NOT NULL UNIQUE,
    parent_id NUMBER(20) DEFAULT 0,
    applicable_types CLOB,
    description VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    sort_order NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE config_category IS '配置分类表';
COMMENT ON COLUMN config_category.id IS '主键ID';
COMMENT ON COLUMN config_category.category_name IS '分类名称';
COMMENT ON COLUMN config_category.category_code IS '分类编码';
COMMENT ON COLUMN config_category.parent_id IS '父分类ID';
COMMENT ON COLUMN config_category.applicable_types IS '适用服务器类型ID列表，逗号分隔';
COMMENT ON COLUMN config_category.description IS '分类描述';
COMMENT ON COLUMN config_category.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN config_category.sort_order IS '排序号';
COMMENT ON COLUMN config_category.create_time IS '创建时间';
COMMENT ON COLUMN config_category.update_time IS '更新时间';
COMMENT ON COLUMN config_category.create_by IS '创建人';
COMMENT ON COLUMN config_category.update_by IS '更新人';

CREATE SEQUENCE seq_config_category
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_config_category
BEFORE INSERT ON config_category
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_config_category.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_config_category_code ON config_category(category_code);
CREATE INDEX idx_config_category_parent_id ON config_category(parent_id);
CREATE INDEX idx_config_category_status ON config_category(status);

-- ===================================
-- 3. 基线管理表
-- ===================================

-- 配置基线表
CREATE TABLE config_baseline (
    id NUMBER(20) PRIMARY KEY,
    system_id NUMBER(20) NOT NULL,
    server_type_id NUMBER(20) NOT NULL,
    category_id NUMBER(20) NOT NULL,
    baseline_name VARCHAR2(200) NOT NULL,
    baseline_version VARCHAR2(50) NOT NULL,
    file_name VARCHAR2(200),
    config_content CLOB NOT NULL,
    config_hash VARCHAR2(64),
    is_default NUMBER(1) DEFAULT 0,
    status NUMBER(1) DEFAULT 0,
    description CLOB,
    source_type VARCHAR2(20) DEFAULT 'MANUAL',
    source_baseline_id NUMBER(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100),
    CONSTRAINT uk_config_baseline UNIQUE (system_id, server_type_id, category_id, is_default)
);

-- 添加表注释
COMMENT ON TABLE config_baseline IS '配置基线表';
COMMENT ON COLUMN config_baseline.id IS '主键ID';
COMMENT ON COLUMN config_baseline.system_id IS '系统ID';
COMMENT ON COLUMN config_baseline.server_type_id IS '服务器类型ID';
COMMENT ON COLUMN config_baseline.category_id IS '配置分类ID';
COMMENT ON COLUMN config_baseline.baseline_name IS '基线名称';
COMMENT ON COLUMN config_baseline.baseline_version IS '基线版本';
COMMENT ON COLUMN config_baseline.file_name IS '原始文件名';
COMMENT ON COLUMN config_baseline.config_content IS '配置内容';
COMMENT ON COLUMN config_baseline.config_hash IS '配置内容哈希值';
COMMENT ON COLUMN config_baseline.is_default IS '是否默认版本：1是 0否';
COMMENT ON COLUMN config_baseline.status IS '状态：0草稿 1生效 2归档';
COMMENT ON COLUMN config_baseline.description IS '基线描述';
COMMENT ON COLUMN config_baseline.source_type IS '来源类型：MANUAL/IMPORT/COPY';
COMMENT ON COLUMN config_baseline.source_baseline_id IS '来源基线ID（复制时使用）';
COMMENT ON COLUMN config_baseline.create_time IS '创建时间';
COMMENT ON COLUMN config_baseline.update_time IS '更新时间';
COMMENT ON COLUMN config_baseline.create_by IS '创建人';
COMMENT ON COLUMN config_baseline.update_by IS '更新人';

CREATE SEQUENCE seq_config_baseline
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_config_baseline
BEFORE INSERT ON config_baseline
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_config_baseline.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_config_baseline_system_id ON config_baseline(system_id);
CREATE INDEX idx_config_baseline_type_id ON config_baseline(server_type_id);
CREATE INDEX idx_config_baseline_category_id ON config_baseline(category_id);
CREATE INDEX idx_config_baseline_version ON config_baseline(baseline_version);
CREATE INDEX idx_config_baseline_status ON config_baseline(status);
CREATE INDEX idx_config_baseline_default ON config_baseline(is_default);
CREATE INDEX idx_config_baseline_hash ON config_baseline(config_hash);

-- 基线版本切换日志表
CREATE TABLE baseline_version_log (
    id NUMBER(20) PRIMARY KEY,
    system_id NUMBER(20) NOT NULL,
    server_type_id NUMBER(20) NOT NULL,
    category_id NUMBER(20) NOT NULL,
    old_baseline_id NUMBER(20),
    new_baseline_id NUMBER(20) NOT NULL,
    old_version VARCHAR2(50),
    new_version VARCHAR2(50) NOT NULL,
    switch_reason CLOB,
    operation_type VARCHAR2(20) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE baseline_version_log IS '基线版本切换日志表';
COMMENT ON COLUMN baseline_version_log.id IS '主键ID';
COMMENT ON COLUMN baseline_version_log.system_id IS '系统ID';
COMMENT ON COLUMN baseline_version_log.server_type_id IS '服务器类型ID';
COMMENT ON COLUMN baseline_version_log.category_id IS '配置分类ID';
COMMENT ON COLUMN baseline_version_log.old_baseline_id IS '原默认基线ID';
COMMENT ON COLUMN baseline_version_log.new_baseline_id IS '新默认基线ID';
COMMENT ON COLUMN baseline_version_log.old_version IS '原版本号';
COMMENT ON COLUMN baseline_version_log.new_version IS '新版本号';
COMMENT ON COLUMN baseline_version_log.switch_reason IS '切换原因';
COMMENT ON COLUMN baseline_version_log.operation_type IS '操作类型：SWITCH/ROLLBACK';
COMMENT ON COLUMN baseline_version_log.create_time IS '创建时间';
COMMENT ON COLUMN baseline_version_log.create_by IS '操作人';

CREATE SEQUENCE seq_baseline_version_log
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_baseline_version_log
BEFORE INSERT ON baseline_version_log
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_baseline_version_log.NEXTVAL;
    END IF;
END;
/

CREATE INDEX idx_baseline_log_system_id ON baseline_version_log(system_id);
CREATE INDEX idx_baseline_log_type_id ON baseline_version_log(server_type_id);
CREATE INDEX idx_baseline_log_create_time ON baseline_version_log(create_time);

-- ===================================
-- 4. 采集相关表
-- ===================================

-- 采集类型扩展表
CREATE TABLE collect_type_extension (
    id NUMBER(20) PRIMARY KEY,
    type_code VARCHAR2(50) NOT NULL UNIQUE,
    type_name VARCHAR2(100) NOT NULL,
    type_category VARCHAR2(50) NOT NULL,
    handler_class VARCHAR2(200) NOT NULL,
    config_schema CLOB,
    description CLOB,
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE collect_type_extension IS '采集类型扩展表';
COMMENT ON COLUMN collect_type_extension.id IS '主键ID';
COMMENT ON COLUMN collect_type_extension.type_code IS '类型编码';
COMMENT ON COLUMN collect_type_extension.type_name IS '类型名称';
COMMENT ON COLUMN collect_type_extension.type_category IS '类型分类：BASIC/EXTENDED/CUSTOM';
COMMENT ON COLUMN collect_type_extension.handler_class IS '处理器类名';
COMMENT ON COLUMN collect_type_extension.config_schema IS '配置参数架构JSON Schema';
COMMENT ON COLUMN collect_type_extension.description IS '类型描述';
COMMENT ON COLUMN collect_type_extension.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN collect_type_extension.create_time IS '创建时间';
COMMENT ON COLUMN collect_type_extension.update_time IS '更新时间';
COMMENT ON COLUMN collect_type_extension.create_by IS '创建人';
COMMENT ON COLUMN collect_type_extension.update_by IS '更新人';

CREATE SEQUENCE seq_collect_type_extension
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_collect_type_extension
BEFORE INSERT ON collect_type_extension
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_collect_type_extension.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_collect_type_code ON collect_type_extension(type_code);
CREATE INDEX idx_collect_type_category ON collect_type_extension(type_category);
CREATE INDEX idx_collect_type_status ON collect_type_extension(status);

-- 采集参数配置表
CREATE TABLE collect_config_params (
    id NUMBER(20) PRIMARY KEY,
    config_group VARCHAR2(100) NOT NULL,
    param_key VARCHAR2(100) NOT NULL,
    param_value VARCHAR2(500),
    param_type VARCHAR2(20) DEFAULT 'STRING',
    is_encrypted NUMBER(1) DEFAULT 0,
    description VARCHAR2(200),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_collect_config_params UNIQUE (config_group, param_key)
);

-- 添加表注释
COMMENT ON TABLE collect_config_params IS '采集参数配置表';
COMMENT ON COLUMN collect_config_params.id IS '主键ID';
COMMENT ON COLUMN collect_config_params.config_group IS '配置组：APOLLO/DATABASE/CUSTOM';
COMMENT ON COLUMN collect_config_params.param_key IS '参数键';
COMMENT ON COLUMN collect_config_params.param_value IS '参数值';
COMMENT ON COLUMN collect_config_params.param_type IS '参数类型：STRING/INT/BOOLEAN/JSON';
COMMENT ON COLUMN collect_config_params.is_encrypted IS '是否加密：1是 0否';
COMMENT ON COLUMN collect_config_params.description IS '参数描述';
COMMENT ON COLUMN collect_config_params.create_time IS '创建时间';
COMMENT ON COLUMN collect_config_params.update_time IS '更新时间';

CREATE SEQUENCE seq_collect_config_params
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_collect_config_params
BEFORE INSERT ON collect_config_params
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_collect_config_params.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

-- 采集模板表
CREATE TABLE collect_template (
    id NUMBER(20) PRIMARY KEY,
    template_name VARCHAR2(200) NOT NULL,
    template_type VARCHAR2(50) NOT NULL,
    template_content CLOB NOT NULL,
    applicable_server_types CLOB,
    config_params CLOB,
    description CLOB,
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE collect_template IS '采集模板表';
COMMENT ON COLUMN collect_template.id IS '主键ID';
COMMENT ON COLUMN collect_template.template_name IS '模板名称';
COMMENT ON COLUMN collect_template.template_type IS '模板类型：SERVER_CONFIG/FILE_CONFIG/API_CONFIG/APOLLO/MULTI_TYPE';
COMMENT ON COLUMN collect_template.template_content IS '模板内容JSON';
COMMENT ON COLUMN collect_template.applicable_server_types IS '适用服务器类型列表';
COMMENT ON COLUMN collect_template.config_params IS '扩展配置参数JSON';
COMMENT ON COLUMN collect_template.description IS '模板描述';
COMMENT ON COLUMN collect_template.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN collect_template.create_time IS '创建时间';
COMMENT ON COLUMN collect_template.update_time IS '更新时间';
COMMENT ON COLUMN collect_template.create_by IS '创建人';
COMMENT ON COLUMN collect_template.update_by IS '更新人';

CREATE SEQUENCE seq_collect_template
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_collect_template
BEFORE INSERT ON collect_template
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_collect_template.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_collect_template_type ON collect_template(template_type);
CREATE INDEX idx_collect_template_status ON collect_template(status);

-- 采集任务表
CREATE TABLE collect_task (
    id NUMBER(20) PRIMARY KEY,
    task_name VARCHAR2(200) NOT NULL,
    system_id NUMBER(20) NOT NULL,
    server_type_ids CLOB,
    server_instance_ids CLOB,
    template_id NUMBER(20) NOT NULL,
    cron_expression VARCHAR2(100),
    execute_type NUMBER(1) NOT NULL,
    max_concurrency NUMBER(10) DEFAULT 5,
    timeout_seconds NUMBER(10) DEFAULT 300,
    retry_count NUMBER(10) DEFAULT 2,
    status NUMBER(1) DEFAULT 1,
    last_execute_time TIMESTAMP,
    next_execute_time TIMESTAMP,
    description CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE collect_task IS '采集任务表';
COMMENT ON COLUMN collect_task.id IS '主键ID';
COMMENT ON COLUMN collect_task.task_name IS '任务名称';
COMMENT ON COLUMN collect_task.system_id IS '系统ID';
COMMENT ON COLUMN collect_task.server_type_ids IS '服务器类型ID列表，逗号分隔';
COMMENT ON COLUMN collect_task.server_instance_ids IS '服务器实例ID列表，逗号分隔';
COMMENT ON COLUMN collect_task.template_id IS '模板ID';
COMMENT ON COLUMN collect_task.cron_expression IS 'Cron表达式';
COMMENT ON COLUMN collect_task.execute_type IS '执行类型：1立即执行 2定时执行';
COMMENT ON COLUMN collect_task.max_concurrency IS '最大并发数';
COMMENT ON COLUMN collect_task.timeout_seconds IS '超时时间（秒）';
COMMENT ON COLUMN collect_task.retry_count IS '重试次数';
COMMENT ON COLUMN collect_task.status IS '任务状态：1启用 0禁用';
COMMENT ON COLUMN collect_task.last_execute_time IS '最后执行时间';
COMMENT ON COLUMN collect_task.next_execute_time IS '下次执行时间';
COMMENT ON COLUMN collect_task.description IS '任务描述';
COMMENT ON COLUMN collect_task.create_time IS '创建时间';
COMMENT ON COLUMN collect_task.update_time IS '更新时间';
COMMENT ON COLUMN collect_task.create_by IS '创建人';
COMMENT ON COLUMN collect_task.update_by IS '更新人';

CREATE SEQUENCE seq_collect_task
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_collect_task
BEFORE INSERT ON collect_task
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_collect_task.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_collect_task_system_id ON collect_task(system_id);
CREATE INDEX idx_collect_task_status ON collect_task(status);
CREATE INDEX idx_collect_task_next_time ON collect_task(next_execute_time);

-- 采集执行记录表
CREATE TABLE collect_execution (
    id NUMBER(20) PRIMARY KEY,
    task_id NUMBER(20) NOT NULL,
    execute_id VARCHAR2(50) NOT NULL,
    execute_status NUMBER(1) NOT NULL,
    total_servers NUMBER(10) NOT NULL,
    success_servers NUMBER(10) DEFAULT 0,
    failed_servers NUMBER(10) DEFAULT 0,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms NUMBER(20),
    error_message CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execute_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE collect_execution IS '采集执行记录表';
COMMENT ON COLUMN collect_execution.id IS '主键ID';
COMMENT ON COLUMN collect_execution.task_id IS '任务ID';
COMMENT ON COLUMN collect_execution.execute_id IS '执行ID';
COMMENT ON COLUMN collect_execution.execute_status IS '执行状态：1成功 2部分成功 3失败 4运行中';
COMMENT ON COLUMN collect_execution.total_servers IS '总服务器数';
COMMENT ON COLUMN collect_execution.success_servers IS '成功服务器数';
COMMENT ON COLUMN collect_execution.failed_servers IS '失败服务器数';
COMMENT ON COLUMN collect_execution.start_time IS '开始时间';
COMMENT ON COLUMN collect_execution.end_time IS '结束时间';
COMMENT ON COLUMN collect_execution.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN collect_execution.error_message IS '错误信息';
COMMENT ON COLUMN collect_execution.create_time IS '创建时间';
COMMENT ON COLUMN collect_execution.update_time IS '更新时间';
COMMENT ON COLUMN collect_execution.execute_by IS '执行人';

CREATE SEQUENCE seq_collect_execution
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_collect_execution
BEFORE INSERT ON collect_execution
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_collect_execution.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_collect_execution_task_id ON collect_execution(task_id);
CREATE INDEX idx_collect_execution_id ON collect_execution(execute_id);
CREATE INDEX idx_collect_execution_start_time ON collect_execution(start_time);
CREATE INDEX idx_collect_execution_status ON collect_execution(execute_status);

-- 采集结果表
CREATE TABLE collect_result (
    id NUMBER(20) PRIMARY KEY,
    task_id NUMBER(20) NOT NULL,
    execute_id VARCHAR2(50) NOT NULL,
    server_instance_id NUMBER(20) NOT NULL,
    collect_item_name VARCHAR2(200) NOT NULL,
    collect_type VARCHAR2(50) NOT NULL,
    collect_content CLOB,
    file_path VARCHAR2(500),
    api_endpoint VARCHAR2(500),
    namespace VARCHAR2(100),
    collect_status NUMBER(1) NOT NULL,
    error_message CLOB,
    execute_time TIMESTAMP NOT NULL,
    duration_ms NUMBER(20),
    retry_count NUMBER(10) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加表注释
COMMENT ON TABLE collect_result IS '采集结果表';
COMMENT ON COLUMN collect_result.id IS '主键ID';
COMMENT ON COLUMN collect_result.task_id IS '任务ID';
COMMENT ON COLUMN collect_result.execute_id IS '执行ID';
COMMENT ON COLUMN collect_result.server_instance_id IS '服务器实例ID';
COMMENT ON COLUMN collect_result.collect_item_name IS '采集项名称';
COMMENT ON COLUMN collect_result.collect_type IS '采集类型：COMMAND/FILE/API/APOLLO';
COMMENT ON COLUMN collect_result.collect_content IS '采集内容';
COMMENT ON COLUMN collect_result.file_path IS '文件路径（文件采集时使用）';
COMMENT ON COLUMN collect_result.api_endpoint IS 'API端点（API采集时使用）';
COMMENT ON COLUMN collect_result.namespace IS '命名空间（Apollo采集时使用）';
COMMENT ON COLUMN collect_result.collect_status IS '采集状态：1成功 0失败';
COMMENT ON COLUMN collect_result.error_message IS '错误信息';
COMMENT ON COLUMN collect_result.execute_time IS '执行时间';
COMMENT ON COLUMN collect_result.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN collect_result.retry_count IS '重试次数';
COMMENT ON COLUMN collect_result.create_time IS '创建时间';
COMMENT ON COLUMN collect_result.update_time IS '更新时间';

CREATE SEQUENCE seq_collect_result
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_collect_result
BEFORE INSERT ON collect_result
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_collect_result.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_collect_result_task_id ON collect_result(task_id);
CREATE INDEX idx_collect_result_execute_id ON collect_result(execute_id);
CREATE INDEX idx_collect_result_instance_id ON collect_result(server_instance_id);
CREATE INDEX idx_collect_result_execute_time ON collect_result(execute_time);
CREATE INDEX idx_collect_result_type ON collect_result(collect_type);
CREATE INDEX idx_collect_result_status ON collect_result(collect_status);

-- ===================================
-- 5. 比对相关表
-- ===================================

-- 比对任务表
CREATE TABLE compare_task (
    id NUMBER(20) PRIMARY KEY,
    task_name VARCHAR2(200) NOT NULL,
    system_id NUMBER(20) NOT NULL,
    server_type_id NUMBER(20) NOT NULL,
    category_id NUMBER(20) NOT NULL,
    baseline_id NUMBER(20),
    target_server_ids CLOB,
    collect_task_id NUMBER(20),
    compare_rules CLOB,
    execute_type NUMBER(1) NOT NULL,
    cron_expression VARCHAR2(100),
    auto_execute NUMBER(1) DEFAULT 0,
    status NUMBER(1) DEFAULT 1,
    last_execute_time TIMESTAMP,
    next_execute_time TIMESTAMP,
    description CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE compare_task IS '比对任务表';
COMMENT ON COLUMN compare_task.id IS '主键ID';
COMMENT ON COLUMN compare_task.task_name IS '任务名称';
COMMENT ON COLUMN compare_task.system_id IS '系统ID';
COMMENT ON COLUMN compare_task.server_type_id IS '服务器类型ID';
COMMENT ON COLUMN compare_task.category_id IS '配置分类ID';
COMMENT ON COLUMN compare_task.baseline_id IS '指定基线ID（为空则使用默认基线）';
COMMENT ON COLUMN compare_task.target_server_ids IS '目标服务器ID列表，逗号分隔';
COMMENT ON COLUMN compare_task.collect_task_id IS '关联采集任务ID';
COMMENT ON COLUMN compare_task.compare_rules IS '比对规则JSON';
COMMENT ON COLUMN compare_task.execute_type IS '执行类型：1立即执行 2定时执行 3触发执行';
COMMENT ON COLUMN compare_task.cron_expression IS 'Cron表达式';
COMMENT ON COLUMN compare_task.auto_execute IS '是否自动执行：1是 0否';
COMMENT ON COLUMN compare_task.status IS '任务状态：1启用 0禁用';
COMMENT ON COLUMN compare_task.last_execute_time IS '最后执行时间';
COMMENT ON COLUMN compare_task.next_execute_time IS '下次执行时间';
COMMENT ON COLUMN compare_task.description IS '任务描述';
COMMENT ON COLUMN compare_task.create_time IS '创建时间';
COMMENT ON COLUMN compare_task.update_time IS '更新时间';
COMMENT ON COLUMN compare_task.create_by IS '创建人';
COMMENT ON COLUMN compare_task.update_by IS '更新人';

CREATE SEQUENCE seq_compare_task
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_compare_task
BEFORE INSERT ON compare_task
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_compare_task.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_compare_task_system_id ON compare_task(system_id);
CREATE INDEX idx_compare_task_type_id ON compare_task(server_type_id);
CREATE INDEX idx_compare_task_category_id ON compare_task(category_id);
CREATE INDEX idx_compare_task_status ON compare_task(status);
CREATE INDEX idx_compare_task_next_time ON compare_task(next_execute_time);

-- 比对执行记录表
CREATE TABLE compare_execution (
    id NUMBER(20) PRIMARY KEY,
    task_id NUMBER(20) NOT NULL,
    execute_id VARCHAR2(50) NOT NULL,
    baseline_id NUMBER(20) NOT NULL,
    baseline_version VARCHAR2(50) NOT NULL,
    execute_status NUMBER(1) NOT NULL,
    total_servers NUMBER(10) NOT NULL,
    consistent_servers NUMBER(10) DEFAULT 0,
    inconsistent_servers NUMBER(10) DEFAULT 0,
    failed_servers NUMBER(10) DEFAULT 0,
    overall_score NUMBER(5,2),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_ms NUMBER(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    execute_by VARCHAR2(100),
    error_message CLOB
);

-- 添加表注释
COMMENT ON TABLE compare_execution IS '比对执行记录表';
COMMENT ON COLUMN compare_execution.id IS '主键ID';
COMMENT ON COLUMN compare_execution.task_id IS '比对任务ID';
COMMENT ON COLUMN compare_execution.execute_id IS '执行ID';
COMMENT ON COLUMN compare_execution.baseline_id IS '使用的基线ID';
COMMENT ON COLUMN compare_execution.baseline_version IS '基线版本';
COMMENT ON COLUMN compare_execution.execute_status IS '执行状态：1成功 2部分成功 3失败';
COMMENT ON COLUMN compare_execution.total_servers IS '总服务器数';
COMMENT ON COLUMN compare_execution.consistent_servers IS '一致服务器数';
COMMENT ON COLUMN compare_execution.inconsistent_servers IS '不一致服务器数';
COMMENT ON COLUMN compare_execution.failed_servers IS '比对失败服务器数';
COMMENT ON COLUMN compare_execution.overall_score IS '整体一致性评分';
COMMENT ON COLUMN compare_execution.start_time IS '开始时间';
COMMENT ON COLUMN compare_execution.end_time IS '结束时间';
COMMENT ON COLUMN compare_execution.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN compare_execution.create_time IS '创建时间';
COMMENT ON COLUMN compare_execution.update_time IS '更新时间';
COMMENT ON COLUMN compare_execution.execute_by IS '执行人';
COMMENT ON COLUMN compare_execution.error_message IS '错误信息';

CREATE SEQUENCE seq_compare_execution
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_compare_execution
BEFORE INSERT ON compare_execution
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_compare_execution.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_compare_execution_task_id ON compare_execution(task_id);
CREATE INDEX idx_compare_execution_id ON compare_execution(execute_id);
CREATE INDEX idx_compare_execution_start_time ON compare_execution(start_time);
CREATE INDEX idx_compare_execution_status ON compare_execution(execute_status);
CREATE INDEX idx_compare_execution_create_time ON compare_execution(create_time);
CREATE INDEX idx_compare_execution_update_time ON compare_execution(update_time);

-- 比对结果表
CREATE TABLE compare_result (
    id NUMBER(20) PRIMARY KEY,
    task_id NUMBER(20) NOT NULL,
    execute_id VARCHAR2(50) NOT NULL,
    baseline_id NUMBER(20) NOT NULL,
    server_instance_id NUMBER(20) NOT NULL,
    collect_result_id NUMBER(20) NOT NULL,
    compare_status NUMBER(1) NOT NULL,
    consistency_score NUMBER(5,2),
    diff_count NUMBER(10) DEFAULT 0,
    high_diff_count NUMBER(10) DEFAULT 0,
    medium_diff_count NUMBER(10) DEFAULT 0,
    low_diff_count NUMBER(10) DEFAULT 0,
    diff_summary CLOB,
    execute_time TIMESTAMP NOT NULL,
    duration_ms NUMBER(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    error_message CLOB
);

-- 添加表注释
COMMENT ON TABLE compare_result IS '比对结果表';
COMMENT ON COLUMN compare_result.id IS '主键ID';
COMMENT ON COLUMN compare_result.task_id IS '比对任务ID';
COMMENT ON COLUMN compare_result.execute_id IS '执行ID';
COMMENT ON COLUMN compare_result.baseline_id IS '基线ID';
COMMENT ON COLUMN compare_result.server_instance_id IS '服务器实例ID';
COMMENT ON COLUMN compare_result.collect_result_id IS '采集结果ID';
COMMENT ON COLUMN compare_result.compare_status IS '比对状态：1一致 0不一致 -1比对失败';
COMMENT ON COLUMN compare_result.consistency_score IS '一致性评分';
COMMENT ON COLUMN compare_result.diff_count IS '差异数量';
COMMENT ON COLUMN compare_result.high_diff_count IS '高级别差异数量';
COMMENT ON COLUMN compare_result.medium_diff_count IS '中级别差异数量';
COMMENT ON COLUMN compare_result.low_diff_count IS '低级别差异数量';
COMMENT ON COLUMN compare_result.diff_summary IS '差异摘要JSON';
COMMENT ON COLUMN compare_result.execute_time IS '执行时间';
COMMENT ON COLUMN compare_result.duration_ms IS '执行耗时(毫秒)';
COMMENT ON COLUMN compare_result.create_time IS '创建时间';
COMMENT ON COLUMN compare_result.update_time IS '更新时间';
COMMENT ON COLUMN compare_result.error_message IS '错误信息';

CREATE SEQUENCE seq_compare_result
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_compare_result
BEFORE INSERT ON compare_result
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_compare_result.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_compare_result_task_id ON compare_result(task_id);
CREATE INDEX idx_compare_result_execute_id ON compare_result(execute_id);
CREATE INDEX idx_compare_result_instance_id ON compare_result(server_instance_id);
CREATE INDEX idx_compare_result_execute_time ON compare_result(execute_time);
CREATE INDEX idx_compare_result_status ON compare_result(compare_status);
CREATE INDEX idx_compare_result_create_time ON compare_result(create_time);
CREATE INDEX idx_compare_result_update_time ON compare_result(update_time);

-- 差异详情表
CREATE TABLE diff_detail (
    id NUMBER(20) PRIMARY KEY,
    result_id NUMBER(20) NOT NULL,
    diff_type VARCHAR2(50) NOT NULL,
    diff_path VARCHAR2(500),
    diff_key VARCHAR2(200),
    baseline_value CLOB,
    current_value CLOB,
    diff_level VARCHAR2(20) NOT NULL,
    diff_category VARCHAR2(100),
    suggest_action VARCHAR2(200)
);

-- 添加表注释
COMMENT ON TABLE diff_detail IS '差异详情表';
COMMENT ON COLUMN diff_detail.id IS '主键ID';
COMMENT ON COLUMN diff_detail.result_id IS '比对结果ID';
COMMENT ON COLUMN diff_detail.diff_type IS '差异类型：ADD/DELETE/MODIFY';
COMMENT ON COLUMN diff_detail.diff_path IS '差异路径';
COMMENT ON COLUMN diff_detail.diff_key IS '差异键名';
COMMENT ON COLUMN diff_detail.baseline_value IS '基线值';
COMMENT ON COLUMN diff_detail.current_value IS '当前值';
COMMENT ON COLUMN diff_detail.diff_level IS '差异级别：HIGH/MEDIUM/LOW';
COMMENT ON COLUMN diff_detail.diff_category IS '差异分类';
COMMENT ON COLUMN diff_detail.suggest_action IS '建议操作';

CREATE SEQUENCE seq_diff_detail
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_diff_detail
BEFORE INSERT ON diff_detail
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_diff_detail.NEXTVAL;
    END IF;
END;
/

CREATE INDEX idx_diff_detail_result_id ON diff_detail(result_id);
CREATE INDEX idx_diff_detail_level ON diff_detail(diff_level);
CREATE INDEX idx_diff_detail_type ON diff_detail(diff_type);

-- ===================================
-- 6. 报告相关表
-- ===================================

-- 报告模板表
CREATE TABLE report_template (
    id NUMBER(20) PRIMARY KEY,
    template_name VARCHAR2(200) NOT NULL,
    template_type VARCHAR2(50) NOT NULL,
    template_content CLOB NOT NULL,
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100),
    update_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE report_template IS '报告模板表';
COMMENT ON COLUMN report_template.id IS '主键ID';
COMMENT ON COLUMN report_template.template_name IS '模板名称';
COMMENT ON COLUMN report_template.template_type IS '模板类型';
COMMENT ON COLUMN report_template.template_content IS '模板内容';
COMMENT ON COLUMN report_template.status IS '状态：1启用 0禁用';
COMMENT ON COLUMN report_template.create_time IS '创建时间';
COMMENT ON COLUMN report_template.update_time IS '更新时间';
COMMENT ON COLUMN report_template.create_by IS '创建人';
COMMENT ON COLUMN report_template.update_by IS '更新人';

CREATE SEQUENCE seq_report_template
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_report_template
BEFORE INSERT ON report_template
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_report_template.NEXTVAL;
    END IF;
    :new.update_time := CURRENT_TIMESTAMP;
END;
/

CREATE INDEX idx_report_template_type ON report_template(template_type);
CREATE INDEX idx_report_template_status ON report_template(status);

-- 报告记录表
CREATE TABLE report_record (
    id NUMBER(20) PRIMARY KEY,
    report_name VARCHAR2(200) NOT NULL,
    report_type VARCHAR2(50) NOT NULL,
    system_ids CLOB,
    time_range VARCHAR2(100),
    report_content CLOB,
    file_path VARCHAR2(500),
    status NUMBER(1) DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR2(100)
);

-- 添加表注释
COMMENT ON TABLE report_record IS '报告记录表';
COMMENT ON COLUMN report_record.id IS '主键ID';
COMMENT ON COLUMN report_record.report_name IS '报告名称';
COMMENT ON COLUMN report_record.report_type IS '报告类型';
COMMENT ON COLUMN report_record.system_ids IS '涉及系统ID列表';
COMMENT ON COLUMN report_record.time_range IS '时间范围';
COMMENT ON COLUMN report_record.report_content IS '报告内容JSON';
COMMENT ON COLUMN report_record.file_path IS '报告文件路径';
COMMENT ON COLUMN report_record.status IS '状态：1正常 0已删除';
COMMENT ON COLUMN report_record.create_time IS '创建时间';
COMMENT ON COLUMN report_record.create_by IS '创建人';

CREATE SEQUENCE seq_report_record
START WITH 1
INCREMENT BY 1
NOCACHE
NOCYCLE;

CREATE OR REPLACE TRIGGER trg_report_record
BEFORE INSERT ON report_record
FOR EACH ROW
BEGIN
    IF :new.id IS NULL THEN
        :new.id := seq_report_record.NEXTVAL;
    END IF;
END;
/

CREATE INDEX idx_report_record_type ON report_record(report_type);
CREATE INDEX idx_report_record_create_time ON report_record(create_time);

-- ===================================
-- 7. 初始化数据
-- ===================================

-- 插入默认服务器类型
INSERT INTO server_type (id, type_name, type_code, description, create_by) VALUES
(1, '应用服务器', 'APP_SERVER', '运行应用程序的服务器', 'system'),
(2, '内管服务器', 'ADMIN_SERVER', '内部管理和监控的服务器', 'system'),
(3, '批量服务器', 'BATCH_SERVER', '处理批量任务的服务器', 'system'),
(4, '数据库服务器', 'DB_SERVER', '数据库服务器', 'system'),
(5, '网关服务器', 'GATEWAY_SERVER', '网关和代理服务器', 'system'),
(6, 'Apollo配置中心', 'APOLLO_CONFIG', 'Apollo配置中心服务', 'system');

-- 插入默认配置分类
INSERT INTO config_category (id, category_name, category_code, applicable_types, description, create_by) VALUES
(1, '应用配置', 'APP_CONFIG', '1,2,3', '微服务应用配置文件', 'system'),
(2, 'JVM配置', 'JVM_CONFIG', '1,2,3', 'Java虚拟机运行参数', 'system'),
(3, '系统资源', 'SYSTEM_RESOURCE', '1,2,3,4,5', '系统资源使用情况', 'system'),
(4, '网络配置', 'NETWORK_CONFIG', '1,2,3,4,5', '网络相关配置', 'system'),
(5, '中间件配置', 'MIDDLEWARE_CONFIG', '1,2,3,4', '中间件相关配置', 'system'),
(6, 'Apollo配置', 'APOLLO_CONFIG', '6', 'Apollo配置中心的配置项', 'system');

-- 插入默认采集类型
INSERT INTO collect_type_extension (id, type_code, type_name, type_category, handler_class, description, create_by) VALUES
(1, 'COMMAND', 'SSH命令执行', 'BASIC', 'com.config.collect.handler.CommandCollectHandler', '通过SSH执行命令采集配置', 'system'),
(2, 'FILE', 'SFTP文件下载', 'BASIC', 'com.config.collect.handler.FileCollectHandler', '通过SFTP下载文件采集配置', 'system'),
(3, 'API', 'HTTP接口调用', 'BASIC', 'com.config.collect.handler.ApiCollectHandler', '通过HTTP接口调用采集配置', 'system'),
(4, 'APOLLO', 'Apollo配置中心', 'EXTENDED', 'com.config.collect.handler.ApolloCollectHandler', '从Apollo配置中心采集配置', 'system'),
(5, 'DATABASE', '数据库查询', 'EXTENDED', 'com.config.collect.handler.DatabaseCollectHandler', '通过数据库查询采集配置', 'system');

COMMIT;