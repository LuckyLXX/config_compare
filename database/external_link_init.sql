-- ===================================
-- 外部链接管理功能初始化脚本
-- 创建时间: 2025-11-28
-- 版本: v1.0.0
-- 适用于MySQL 5.7+ / 8.0+
-- ===================================

USE config_compare;

-- ===================================
-- 1. 创建外部链接管理表
-- ===================================

-- 外部链接管理表
CREATE TABLE IF NOT EXISTS sys_external_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    link_name VARCHAR(100) NOT NULL COMMENT '链接名称/标题',
    link_url VARCHAR(500) NOT NULL COMMENT '链接URL地址',
    icon VARCHAR(50) DEFAULT 'Link' COMMENT '图标名称(Element Plus图标)',
    open_type TINYINT DEFAULT 1 COMMENT '打开方式：1-内嵌iframe 2-新窗口打开',
    sort_order INT DEFAULT 0 COMMENT '排序序号，数字越小越靠前',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID，0表示一级菜单',
    description VARCHAR(500) COMMENT '链接描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by VARCHAR(100) COMMENT '创建人',
    update_by VARCHAR(100) COMMENT '更新人',
    INDEX idx_parent_id (parent_id),
    INDEX idx_sort_order (sort_order),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='外部链接管理表';

-- ===================================
-- 2. 字段说明
-- ===================================
-- id: 主键ID，自增
-- link_name: 链接名称，显示在左侧菜单中
-- link_url: 完整的URL地址，支持http/https
-- icon: Element Plus图标名称，如 'Link', 'Setting', 'Monitor' 等
-- open_type: 打开方式
--   1 - 内嵌iframe：在系统内部以iframe方式展示外部页面
--   2 - 新窗口打开：点击后在浏览器新标签页打开
-- sort_order: 排序序号，数字越小排在越前面
-- parent_id: 父级ID，用于支持二级菜单结构
--   0 - 表示一级菜单项
--   其他值 - 表示该链接属于对应ID的父级菜单
-- description: 链接描述，可用于tooltip提示
-- status: 状态控制，1启用/0禁用

-- ===================================
-- 3. 示例数据（可选）
-- ===================================

-- 插入示例外部链接
INSERT INTO sys_external_link (link_name, link_url, icon, open_type, sort_order, description, status) VALUES
('Apollo配置中心', 'http://apollo.example.com', 'Setting', 1, 10, 'Apollo分布式配置管理平台', 1),
('Jenkins', 'http://jenkins.example.com', 'Cpu', 2, 20, 'CI/CD持续集成构建平台', 1),
('Grafana监控', 'http://grafana.example.com', 'TrendCharts', 1, 30, '系统运行监控面板', 1),
('Kibana日志', 'http://kibana.example.com', 'Document', 1, 40, 'ELK日志分析平台', 1),
('Nacos注册中心', 'http://nacos.example.com', 'Connection', 2, 50, '服务注册与发现中心', 1);

-- ===================================
-- 4. 常用查询
-- ===================================

-- 查询所有启用的外部链接（按排序）
-- SELECT * FROM sys_external_link WHERE status = 1 ORDER BY sort_order ASC;

-- 查询指定ID的链接详情
-- SELECT * FROM sys_external_link WHERE id = ?;

-- 更新链接状态
-- UPDATE sys_external_link SET status = ?, update_time = NOW(), update_by = ? WHERE id = ?;

-- ===================================
-- 5. 清理脚本（慎用）
-- ===================================
-- DROP TABLE IF EXISTS sys_external_link;
