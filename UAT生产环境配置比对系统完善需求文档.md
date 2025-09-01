# UAT-生产配置基线与比对系统完善需求文档

## 1. 项目概述

### 1.1 背景与目标
部门内存在多个系统，UAT（准生产）与生产环境在服务器配置、应用配置等存在人为不一致风险，导致投产失败或运行异常。

**项目目标：**
- 以"生产环境"为配置基线，自动采集UAT/生产配置并进行比对
- 输出差异与门禁结果，降低投产风险
- 提供可视化的配置管理和比对结果展示
- 支持定时自动化比对和告警机制

**注意事项：**
当前生产环境无法在线部署采集，仅能通过人工获取文件或命令输出，再在测试环境录入形成基线。

### 1.2 技术架构
- **前端：** Vue.js + Element UI + Axios
- **后端：** Spring Boot + MyBatis Plus + JDK 11
- **数据库：** MySQL 8.0+
- **其他：** SSH客户端、SFTP客户端、定时任务调度

## 2. 系统功能详细设计

### 2.1 系统信息配置页面

#### 2.1.1 功能描述
管理系统的基本信息和连接配置，用于后续的配置采集和比对。支持多服务器类型和多实例管理。

#### 2.1.2 页面功能
- **系统列表展示**
  - 支持分页查询
  - 支持按系统名称、IP地址模糊搜索
  - 显示系统状态（启用/禁用）
  - 支持按服务器类型分组显示
  
- **系统新增/编辑**
  - 系统名称（必填，唯一）
  - 系统描述（选填）
  - 环境类型（UAT/生产，必选）
  - **多服务器类型管理**
    - 服务器类型（应用服务器/内管服务器/数据库服务器/网关服务器等，可自定义）
    - 每种类型支持配置多台服务器
    - 服务器实例管理（IP地址、端口、用户名、密码）
    - 服务器角色标识（主/从/备用等）
  - 系统负责人（选填）
  - 联系方式（选填）
  - 状态（启用/禁用）

- **服务器实例管理**
  - 支持批量添加服务器实例
  - 服务器实例分组（按类型、角色等）
  - 单独的连接测试功能
  - 服务器状态监控

- **连接测试功能**
  - 测试SSH连接是否正常
  - 测试SFTP连接是否正常
  - 显示连接测试结果

#### 2.1.3 数据表设计
```sql
-- 系统信息表（修改）
CREATE TABLE sys_system_info (
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
    update_by VARCHAR(100) COMMENT '更新人'
) COMMENT='系统信息表';

-- 服务器类型表（新增）
CREATE TABLE server_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_name VARCHAR(100) NOT NULL COMMENT '服务器类型名称',
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '服务器类型编码',
    description VARCHAR(500) COMMENT '类型描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '创建人'
) COMMENT='服务器类型表';

-- 服务器实例表（新增）
CREATE TABLE server_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    system_id BIGINT NOT NULL COMMENT '系统ID',
    server_type_id BIGINT NOT NULL COMMENT '服务器类型ID',
    instance_name VARCHAR(200) NOT NULL COMMENT '实例名称',
    server_ip VARCHAR(50) NOT NULL COMMENT '服务器IP',
    ssh_port INT DEFAULT 22 COMMENT 'SSH端口',
    username VARCHAR(100) NOT NULL COMMENT '连接用户名',
    password VARCHAR(500) NOT NULL COMMENT '连接密码（加密存储）',
    server_role VARCHAR(50) COMMENT '服务器角色：MASTER/SLAVE/BACKUP',
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
    UNIQUE KEY uk_system_type_ip (system_id, server_type_id, server_ip)
) COMMENT='服务器实例表';
```

### 2.2 基线中心

#### 2.2.1 功能描述
管理生产环境的配置基线，支持多种方式录入配置数据。基线按服务器类型和配置分类进行组织管理。

#### 2.2.2 页面功能
- **基线分层管理**
  - **系统级别**：按系统分组展示
  - **服务器类型级别**：每种服务器类型独立管理基线
  - **配置分类级别**：每种服务器类型下按配置分类管理
  - 树形结构展示：系统 -> 服务器类型 -> 配置分类 -> 基线版本

- **基线列表管理**
  - 按系统、服务器类型、配置分类多级分组展示
  - 支持基线版本管理（版本号自动生成或手动输入）
  - 支持基线状态管理（草稿/生效/归档）
  - 基线创建时间和创建人信息
  - **版本管理功能**
    - 每个系统的每种服务器类型的每个配置分类支持多个基线版本并存
    - 同一时间只能有一个版本作为默认版本
    - 支持基线版本切换（设置为默认版本）
    - 版本切换需要确认操作，避免误操作
    - 显示当前默认版本标识
    - 支持版本比较功能（查看不同版本差异）

- **基线录入方式**
  - **批量录入**：支持选择多个服务器类型同时录入
  - **模板复制**：支持从已有基线复制创建新基线
  - **文件上传**：支持txt、json、xml、properties、yaml等格式
  - **内容粘贴**：提供文本框直接粘贴配置内容
  - **在线编辑**：提供代码编辑器，支持语法高亮

- **基线分类管理**
  - **服务器配置类**：CPU、内存、磁盘、操作系统等
  - **应用配置类**：配置文件、环境变量、JVM参数等
  - **网络配置类**：端口、防火墙、负载均衡等
  - **中间件配置类**：数据库、缓存、消息队列等
  - **Apollo配置类**：各种Apollo配置命名空间
  - **自定义分类**：支持自定义配置分类

- **版本切换管理**
  - 版本切换操作日志记录
  - 切换前后版本对比展示
  - 支持回滚到历史版本
  - 版本切换影响范围提示（会影响哪些比对任务）

#### 2.2.3 数据表设计
```sql
-- 配置分类表（新增）
CREATE TABLE config_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    applicable_types TEXT COMMENT '适用服务器类型ID列表，逗号分隔',
    description VARCHAR(500) COMMENT '分类描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '创建人'
) COMMENT='配置分类表';

-- 配置基线表（修改）
CREATE TABLE config_baseline (
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
    UNIQUE KEY uk_system_type_category_default (system_id, server_type_id, category_id, is_default)
) COMMENT='配置基线表';

-- 基线版本切换日志表（修改）
CREATE TABLE baseline_version_log (
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
) COMMENT='基线版本切换日志表';
```

### 2.3 采集页面

#### 2.3.1 功能描述
创建和管理配置采集任务，支持多服务器、多类型的并行采集和定时执行。

#### 2.3.2 采集任务设计

**2.3.2.1 多服务器采集策略**
- **并行采集**：同一任务可以并行采集多台服务器
- **分组采集**：按服务器类型分组，每组可以独立配置采集参数
- **容错机制**：部分服务器采集失败不影响其他服务器
- **结果聚合**：采集结果按服务器实例分别存储

**2.3.2.2 采集模板增强设计**

**采集类型可扩展设计**
系统支持可扩展的采集类型，通过插件化设计实现新采集类型的快速集成：

**当前支持的基础采集类型：**
- `COMMAND`：SSH命令执行类型
- `FILE`：SFTP文件下载类型
- `API`：HTTP接口调用类型
- `DATABASE`：数据库查询类型（可扩展）
- `APOLLO`：Apollo配置中心类型（可扩展）

**交易中心系统场景示例：**

应用服务器配置采集模板示例：
```json
{
  "templateName": "交易中心-应用服务器配置采集",
  "templateType": "MULTI_TYPE",
  "applicableServerTypes": ["ONLINE_SERVER", "BATCH_SERVER", "ADMIN_SERVER"],
  "collectItems": [
    {
      "itemName": "微服务配置文件",
      "itemType": "FILE",
      "remotePath": "/opt/trading/{microservice}/config/*.properties",
      "localPath": "./download/{serverType}/{serverIp}/{microservice}/",
      "recursive": true,
      "microservices": ["trade-core", "order-service", "risk-control"],
      "timeout": 60
    },
    {
      "itemName": "JVM运行参数",
      "itemType": "COMMAND",
      "command": "ps aux | grep java | grep {microservice} | grep -v grep",
      "timeout": 30,
      "retryCount": 2
    },
    {
      "itemName": "端口监听情况",
      "itemType": "COMMAND", 
      "command": "netstat -tuln | grep :{port}",
      "ports": ["8080", "8081", "8082"],
      "timeout": 15
    }
  ],
  "parallelExecution": true,
  "maxConcurrency": 3
}
```

Apollo配置中心采集模板示例：
```json
{
  "templateName": "交易中心-Apollo配置采集",
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
}
```

**其他采集模板示例：**

**服务器配置类模板**
```json
{
  "templateName": "服务器配置采集",
  "templateType": "SERVER_CONFIG",
  "commands": [
    {
      "name": "CPU信息",
      "command": "lscpu | grep -E \"(^CPU\\(s\\):|Model name|Core\\(s per socket\\))\"",
      "timeout": 30
    },
    {
      "name": "内存信息", 
      "command": "free -h",
      "timeout": 30
    },
    {
      "name": "磁盘信息",
      "command": "df -h",
      "timeout": 30
    },
    {
      "name": "网络信息",
      "command": "netstat -tuln",
      "timeout": 30
    }
  ]
}
```

**系统配置类模板**
```json
{
  "templateName": "应用配置文件采集",
  "templateType": "FILE_CONFIG",
  "filePaths": [
    {
      "name": "应用配置",
      "remotePath": "/opt/app/config/application.properties",
      "localPath": "./download/",
      "recursive": false
    },
    {
      "name": "日志配置",
      "remotePath": "/opt/app/config/logback.xml",
      "localPath": "./download/",
      "recursive": false
    }
  ]
}
```

**接口配置类模板**
```json
{
  "templateName": "Apollo配置采集",
  "templateType": "API_CONFIG",
  "apis": [
    {
      "name": "获取配置列表",
      "url": "http://apollo.example.com/configs/{appId}/{env}/{namespace}",
      "method": "GET",
      "headers": {
        "Authorization": "Bearer {token}"
      },
      "timeout": 60
    }
  ]
}
```

#### 2.3.3 采集任务管理
- **任务创建**
  - 任务名称和描述
  - 选择目标系统
  - 选择服务器类型（支持多选）
  - 选择具体服务器实例（支持全选或指定选择）
  - 选择采集模板
  - 配置执行策略（立即执行/定时执行）
  - 并发控制参数

- **任务执行监控**
  - 实时显示各服务器采集进度
  - 采集成功/失败统计
  - 错误日志详情查看
  - 支持重试失败的服务器

#### 2.3.4 数据表设计
```sql
-- 采集类型扩展表（新增）
CREATE TABLE collect_type_extension (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    type_category VARCHAR(50) NOT NULL COMMENT '类型分类：BASIC/EXTENDED/CUSTOM',
    handler_class VARCHAR(200) NOT NULL COMMENT '处理器类名',
    config_schema LONGTEXT COMMENT '配置参数架构JSON Schema',
    description TEXT COMMENT '类型描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '创建人'
) COMMENT='采集类型扩展表';

-- 采集参数配置表（新增）
CREATE TABLE collect_config_params (
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
) COMMENT='采集参数配置表';

-- 采集模板表（修改）
CREATE TABLE collect_template (
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
    create_by VARCHAR(100) COMMENT '创建人'
) COMMENT='采集模板表';

-- 服务器实例表（扩展，支持Apollo配置）
CREATE TABLE server_instance (
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
    UNIQUE KEY uk_system_type_name (system_id, server_type_id, instance_name)
) COMMENT='服务器实例表';

-- 采集任务表（修改）
CREATE TABLE collect_task (
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
    INDEX idx_system_id (system_id),
    INDEX idx_status (status)
) COMMENT='采集任务表';

-- 采集执行记录表（新增）
CREATE TABLE collect_execution (
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
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_start_time (start_time)
) COMMENT='采集执行记录表';

-- 采集结果表（修改）
CREATE TABLE collect_result (
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
    INDEX idx_collect_type (collect_type)
) COMMENT='采集结果表';
```
CREATE TABLE collect_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execute_id VARCHAR(50) NOT NULL COMMENT '执行ID',
    server_instance_id BIGINT NOT NULL COMMENT '服务器实例ID',
    collect_item_name VARCHAR(200) NOT NULL COMMENT '采集项名称',
    collect_type VARCHAR(50) NOT NULL COMMENT '采集类型：COMMAND/FILE/API',
    collect_content LONGTEXT COMMENT '采集内容',
    file_path VARCHAR(500) COMMENT '文件路径（文件采集时使用）',
    collect_status TINYINT NOT NULL COMMENT '采集状态：1成功 0失败',
    error_message TEXT COMMENT '错误信息',
    execute_time DATETIME NOT NULL COMMENT '执行时间',
    duration_ms BIGINT COMMENT '执行耗时(毫秒)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_server_instance (server_instance_id),
    INDEX idx_execute_time (execute_time)
) COMMENT='采集结果表';
```

### 2.4 比对中心

#### 2.4.1 功能描述
创建配置比对任务，将采集结果与基线进行比对分析。支持一对多的比对模式：一个基线对比多个采集结果。

#### 2.4.2 比对策略设计

**2.4.2.1 比对模式**
- **一对一比对**：指定基线版本 vs 指定服务器采集结果
- **一对多比对**：一个基线版本 vs 同类型多台服务器采集结果
- **批量比对**：多个基线版本 vs 对应类型的所有服务器采集结果

**2.4.2.2 比对规则矩阵**
```json
{
  "baselineMapping": {
    "systemId": 1,
    "serverType": "APP_SERVER",
    "category": "JVM_CONFIG",
    "baselineVersion": "v1.0.0"
  },
  "targetServers": [
    {
      "serverInstanceId": 101,
      "serverIp": "192.168.1.10",
      "serverRole": "MASTER"
    },
    {
      "serverInstanceId": 102,
      "serverIp": "192.168.1.11", 
      "serverRole": "SLAVE"
    }
  ],
  "compareRules": {
    "ignorePattern": ["^#.*", "^\\s*$"],
    "normalizeRules": [],
    "keyIgnoreList": ["timestamp", "lastModified"],
    "diffLevel": {
      "HIGH": ["jvm.heap.size", "database.url"],
      "MEDIUM": ["log.level", "cache.size"],
      "LOW": ["comment", "description"]
    }
  }
}
```

**2.4.2.3 比对规则设计**
- **文本比对**
  - 逐行比对
  - 忽略空行和注释
  - 支持正则表达式过滤
  
- **JSON比对**
  - 结构化比对
  - 支持忽略特定字段
  - 支持值类型验证

- **属性文件比对**
  - 键值对比对
  - 支持忽略特定属性
  - 支持默认值处理

#### 2.4.3 比对任务管理
- **任务配置**
  - 选择系统和服务器类型
  - 选择基线版本（默认使用当前默认版本）
  - 选择目标服务器实例（支持按角色、IP范围等过滤）
  - 配置比对规则
  - 设置差异阈值
  - **版本选择说明**
    - 定时比对任务自动使用当前默认基线版本
    - 手动比对支持选择指定基线版本
    - 基线版本切换后，定时任务会自动使用新的默认版本
    - 支持查看比对使用的具体基线版本信息

- **执行策略**
  - 立即执行
  - 定时执行
  - 触发执行（新采集结果产生时）
  - 链式执行（采集完成后自动比对）

- **结果聚合**
  - 按服务器实例分别展示比对结果
  - 提供汇总视图（整体一致性评分）
  - 支持差异归类和统计分析

#### 2.4.4 数据表设计
```sql
-- 比对任务表（修改）
CREATE TABLE compare_task (
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
    INDEX idx_system_id (system_id),
    INDEX idx_server_type (server_type_id),
    INDEX idx_category (category_id)
) COMMENT='比对任务表';

-- 比对执行记录表（新增）
CREATE TABLE compare_execution (
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
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_start_time (start_time)
) COMMENT='比对执行记录表';

-- 比对结果表（修改）
CREATE TABLE compare_result (
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
    INDEX idx_task_id (task_id),
    INDEX idx_execute_id (execute_id),
    INDEX idx_server_instance (server_instance_id),
    INDEX idx_execute_time (execute_time)
) COMMENT='比对结果表';

-- 差异详情表（修改）
CREATE TABLE diff_detail (
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
    INDEX idx_result_id (result_id),
    INDEX idx_diff_level (diff_level)
) COMMENT='差异详情表';
```

### 2.5 报告中心

#### 2.5.1 功能描述
展示比对结果的综合报告，提供多维度的数据分析和可视化展示。

#### 2.5.2 报告内容设计
- **总览仪表板**
  - 系统健康度评分
  - 配置一致性趋势图
  - 最近比对结果汇总
  - 告警统计

- **详细比对报告**
  - 差异项列表（分级显示）
  - 差异趋势分析
  - 历史比对记录
  - 导出功能（PDF/Excel）

- **统计分析**
  - 按系统统计
  - 按时间维度统计
  - 差异类型分布
  - 处理率统计

#### 2.5.3 数据表设计
```sql
-- 报告模板表
CREATE TABLE report_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模板名称',
    template_type VARCHAR(50) NOT NULL COMMENT '模板类型',
    template_content LONGTEXT NOT NULL COMMENT '模板内容',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '创建人'
) COMMENT='报告模板表';

-- 报告记录表
CREATE TABLE report_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    report_name VARCHAR(200) NOT NULL COMMENT '报告名称',
    report_type VARCHAR(50) NOT NULL COMMENT '报告类型',
    system_ids TEXT COMMENT '涉及系统ID列表',
    time_range VARCHAR(100) COMMENT '时间范围',
    report_content LONGTEXT COMMENT '报告内容JSON',
    file_path VARCHAR(500) COMMENT '报告文件路径',
    status TINYINT DEFAULT 1 COMMENT '状态：1正常 0已删除',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(100) COMMENT '创建人'
) COMMENT='报告记录表';
```

## 3. 接口设计规范

### 3.1 RESTful API 设计
- 统一返回格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 3.2 主要接口列表

#### 3.2.1 系统管理接口
- `GET /api/systems` - 查询系统列表
- `POST /api/systems` - 创建系统
- `PUT /api/systems/{id}` - 更新系统
- `DELETE /api/systems/{id}` - 删除系统
- `POST /api/systems/{id}/test-connection` - 测试连接

**服务器类型管理接口**
- `GET /api/server-types` - 查询服务器类型列表
- `POST /api/server-types` - 创建服务器类型
- `PUT /api/server-types/{id}` - 更新服务器类型
- `DELETE /api/server-types/{id}` - 删除服务器类型

**服务器实例管理接口**
- `GET /api/systems/{systemId}/servers` - 查询系统的服务器实例
- `POST /api/systems/{systemId}/servers` - 添加服务器实例
- `PUT /api/servers/{id}` - 更新服务器实例
- `DELETE /api/servers/{id}` - 删除服务器实例
- `POST /api/servers/{id}/test-connection` - 测试服务器连接
- `POST /api/servers/batch-test` - 批量测试服务器连接
- `GET /api/systems/{systemId}/servers/by-type/{typeId}` - 按类型查询服务器实例

#### 3.2.2 基线管理接口
- `GET /api/baselines` - 查询基线列表
- `POST /api/baselines` - 创建基线
- `PUT /api/baselines/{id}` - 更新基线
- `DELETE /api/baselines/{id}` - 删除基线
- `POST /api/baselines/upload` - 上传基线文件
- `GET /api/baselines/{systemId}/tree` - 获取系统基线树形结构
- `GET /api/baselines/{systemId}/{serverTypeId}/{categoryId}/versions` - 查询指定系统服务器类型配置分类的所有版本
- `POST /api/baselines/{id}/set-default` - 设置为默认版本
- `GET /api/baselines/{systemId}/{serverTypeId}/{categoryId}/default` - 获取默认基线版本
- `POST /api/baselines/compare` - 比较两个基线版本
- `GET /api/baselines/version-logs` - 查询版本切换日志
- `POST /api/baselines/{id}/rollback` - 回滚到指定版本
- `POST /api/baselines/copy` - 复制基线到新版本

**配置分类管理接口**
- `GET /api/config-categories` - 查询配置分类列表
- `POST /api/config-categories` - 创建配置分类
- `PUT /api/config-categories/{id}` - 更新配置分类
- `DELETE /api/config-categories/{id}` - 删除配置分类

#### 3.2.3 采集管理接口
- `GET /api/collect/templates` - 查询采集模板
- `POST /api/collect/templates` - 创建采集模板
- `PUT /api/collect/templates/{id}` - 更新采集模板
- `DELETE /api/collect/templates/{id}` - 删除采集模板
- `GET /api/collect/tasks` - 查询采集任务
- `POST /api/collect/tasks` - 创建采集任务
- `PUT /api/collect/tasks/{id}` - 更新采集任务
- `DELETE /api/collect/tasks/{id}` - 删除采集任务
- `POST /api/collect/tasks/{id}/execute` - 执行采集任务
- `GET /api/collect/tasks/{id}/status` - 查询任务执行状态
- `GET /api/collect/results` - 查询采集结果
- `GET /api/collect/results/{executeId}` - 查询指定执行的采集结果

**采集类型扩展管理接口**
- `GET /api/collect/types` - 查询所有采集类型
- `POST /api/collect/types` - 创建自定义采集类型
- `PUT /api/collect/types/{id}` - 更新采集类型
- `DELETE /api/collect/types/{id}` - 删除采集类型
- `GET /api/collect/types/{typeCode}/schema` - 获取采集类型配置架构

**采集参数配置管理接口**
- `GET /api/collect/config-params` - 查询配置参数列表
- `POST /api/collect/config-params` - 创建配置参数
- `PUT /api/collect/config-params/{id}` - 更新配置参数
- `DELETE /api/collect/config-params/{id}` - 删除配置参数
- `GET /api/collect/config-params/{group}` - 按组查询配置参数

**Apollo配置专用接口**
- `POST /api/collect/apollo/test-connection` - 测试Apollo连接
- `GET /api/collect/apollo/{appId}/namespaces` - 获取应用的命名空间列表
- `GET /api/collect/apollo/{appId}/{namespace}/configs` - 获取指定命名空间的配置
- `POST /api/collect/apollo/preview` - 预览Apollo采集结果

#### 3.2.4 比对管理接口
- `GET /api/compare/tasks` - 查询比对任务
- `POST /api/compare/tasks` - 创建比对任务
- `POST /api/compare/tasks/{id}/execute` - 执行比对任务
- `GET /api/compare/results` - 查询比对结果

#### 3.2.5 报告管理接口
- `GET /api/reports/dashboard` - 获取仪表板数据
- `GET /api/reports/details` - 获取详细报告
- `POST /api/reports/export` - 导出报告

## 4. 部署架构

### 4.1 开发环境
- Node.js 16+ (前端)
- JDK 11 (后端)
- MySQL 8.0+
- Redis (可选，用于缓存)

### 4.2 目录结构
```
config_compare/
├── frontend/          # Vue前端项目
├── backend/           # Spring Boot后端项目
├── database/          # 数据库脚本
├── docs/              # 项目文档
└── deploy/            # 部署脚本
```

## 5. 开发计划

### 5.1 第一阶段（基础功能）
- 系统信息管理
- 基线管理（手动录入）
- 简单的文本比对功能

### 5.2 第二阶段（采集功能）
- 采集模板设计
- SSH/SFTP采集实现
- 采集任务调度

### 5.3 第三阶段（比对增强）
- 智能比对规则
- 比对结果分析
- 报告生成

### 5.4 第四阶段（完善优化）
- 界面优化
- 性能优化
- 功能完善

## 6. 注意事项

1. **数据库设计**：不使用外键约束，通过应用层维护数据一致性
2. **密码存储**：采用AES加密存储连接密码
3. **日志记录**：详细记录操作日志和执行日志
4. **异常处理**：完善的异常处理和错误提示
5. **性能考虑**：大文件处理和大量数据比对的性能优化
6. **扩展性**：预留扩展接口，支持自定义采集和比对规则
7. **基线版本管理**：
   - 各系统的基线版本独立管理，互不影响
   - 版本切换操作需要严格权限控制和审批流程
   - 切换前必须进行影响范围评估和风险提示
   - 需要对正在运行的比对任务进行影响分析
   - 建议在系统维护窗口期进行基线版本切换
8. **采集类型扩展性设计**：
   - 采用插件化架构，支持新采集类型的快速集成
   - 通过collect_type_extension表管理采集类型元数据
   - 每种采集类型对应一个处理器类，实现统一的采集接口
   - 支持自定义配置参数和JSON Schema验证
   - Apollo、数据库等特殊采集类型都可作为扩展类型实现
9. **多服务器架构设计**：
   - 服务器实例表支持多种连接类型（SSH/SFTP/API/Apollo）
   - 通过custom_config字段支持不同类型服务器的个性化配置
   - 采集任务支持按服务器类型分组执行，提高执行效率
   - 容错设计确保单台服务器失败不影响整体任务

## 7. 附录

### 7.1 交易中心系统完整配置示例

**系统基本信息配置：**
```json
{
  "systemName": "交易中心系统",
  "systemDesc": "核心交易系统，支持联机、批量和内管功能",
  "envType": "PROD",
  "owner": "交易中心团队",
  "contact": "trading-team@company.com"
}
```

**服务器类型配置：**
```json
[
  {
    "typeName": "联机服务器",
    "typeCode": "ONLINE_SERVER",
    "description": "处理实时交易请求的服务器"
  },
  {
    "typeName": "批量服务器",
    "typeCode": "BATCH_SERVER", 
    "description": "处理批量交易和对账的服务器"
  },
  {
    "typeName": "内管服务器",
    "typeCode": "ADMIN_SERVER",
    "description": "内部管理和监控的服务器"
  },
  {
    "typeName": "Apollo配置中心",
    "typeCode": "APOLLO_CONFIG",
    "description": "Apollo配置中心服务"
  }
]
```

**服务器实例配置：**
```json
[
  {
    "instanceName": "联机服务器-01",
    "serverType": "ONLINE_SERVER",
    "serverIp": "10.99.121.1",
    "sshPort": 22,
    "username": "appuser",
    "password": "encrypted_password",
    "serverRole": "MASTER",
    "description": "联机主服务器"
  },
  {
    "instanceName": "联机服务器-02",
    "serverType": "ONLINE_SERVER",
    "serverIp": "10.99.121.2",
    "sshPort": 22,
    "username": "appuser",
    "password": "encrypted_password",
    "serverRole": "SLAVE",
    "description": "联机备服务器"
  },
  {
    "instanceName": "批量服务器-01",
    "serverType": "BATCH_SERVER",
    "serverIp": "10.99.122.1",
    "sshPort": 22,
    "username": "batchuser",
    "password": "encrypted_password",
    "serverRole": "MASTER"
  },
  {
    "instanceName": "批量服务器-02",
    "serverType": "BATCH_SERVER",
    "serverIp": "10.99.122.2",
    "sshPort": 22,
    "username": "batchuser",
    "password": "encrypted_password",
    "serverRole": "SLAVE"
  },
  {
    "instanceName": "内管服务器-01",
    "serverType": "ADMIN_SERVER",
    "serverIp": "10.99.123.1",
    "sshPort": 22,
    "username": "adminuser",
    "password": "encrypted_password",
    "serverRole": "MASTER"
  },
  {
    "instanceName": "内管服务器-02",
    "serverType": "ADMIN_SERVER",
    "serverIp": "10.99.123.2",
    "sshPort": 22,
    "username": "adminuser",
    "password": "encrypted_password",
    "serverRole": "SLAVE"
  },
  {
    "instanceName": "Apollo配置中心",
    "serverType": "APOLLO_CONFIG",
    "apolloServerUrl": "http://apollo.trading.com:8080",
    "apolloAppId": "trading-center",
    "apolloCluster": "default",
    "apolloEnv": "PROD",
    "apolloNamespaces": "application,database,redis,mq,risk-control",
    "apolloToken": "apollo_access_token_encrypted",
    "description": "Apollo配置中心服务"
  }
]
```

**配置分类设计：**
```json
[
  {
    "categoryName": "应用配置",
    "categoryCode": "APP_CONFIG",
    "applicableTypes": "ONLINE_SERVER,BATCH_SERVER,ADMIN_SERVER",
    "description": "微服务应用配置文件"
  },
  {
    "categoryName": "JVM配置",
    "categoryCode": "JVM_CONFIG",
    "applicableTypes": "ONLINE_SERVER,BATCH_SERVER,ADMIN_SERVER",
    "description": "Java虚拟机运行参数"
  },
  {
    "categoryName": "系统资源",
    "categoryCode": "SYSTEM_RESOURCE",
    "applicableTypes": "ONLINE_SERVER,BATCH_SERVER,ADMIN_SERVER",
    "description": "系统资源使用情况"
  },
  {
    "categoryName": "Apollo配置",
    "categoryCode": "APOLLO_CONFIG",
    "applicableTypes": "APOLLO_CONFIG",
    "description": "Apollo配置中心的配置项"
  }
]
```

### 7.2 常用配置采集命令
```bash
# 系统信息
uname -a
cat /etc/os-release

# 硬件信息
lscpu
free -h
df -h
lsblk

# 网络信息
ip addr
netstat -tuln
ss -tuln

# 进程信息
ps aux
systemctl list-units --type=service --state=running

# 文件权限
ls -la /etc/passwd
ls -la /opt/app/
```

### 7.2 常用比对规则示例
```json
{
  "ignorePattern": [
    "^#.*",           // 忽略注释行
    "^\\s*$",         // 忽略空行
    ".*timestamp.*"   // 忽略包含timestamp的行
  ],
  "normalizeRules": [
    {
      "pattern": "\\s+",
      "replacement": " "  // 多个空格替换为单个空格
    }
  ],
  "keyIgnoreList": [
    "lastModified",
    "createTime"
  ]
}
```