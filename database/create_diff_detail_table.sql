-- 创建差异详情表
CREATE TABLE IF NOT EXISTS `compare_diff_detail` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `result_id` bigint(20) NOT NULL COMMENT '比对结果ID',
  `diff_type` varchar(20) NOT NULL COMMENT '差异类型：ADD/DELETE/MODIFY',
  `diff_key` varchar(255) NOT NULL COMMENT '差异键',
  `diff_level` varchar(20) NOT NULL COMMENT '差异级别：HIGH/MEDIUM/LOW',
  `diff_category` varchar(100) DEFAULT NULL COMMENT '差异分类',
  `description` text COMMENT '差异描述',
  `baseline_value` text COMMENT '基线值',
  `current_value` text COMMENT '当前值',
  `suggest_action` varchar(500) DEFAULT NULL COMMENT '建议操作',
  `diff_path` varchar(255) DEFAULT NULL COMMENT '差异路径',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_result_id` (`result_id`),
  KEY `idx_diff_type` (`diff_type`),
  KEY `idx_diff_level` (`diff_level`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比对差异详情表';
