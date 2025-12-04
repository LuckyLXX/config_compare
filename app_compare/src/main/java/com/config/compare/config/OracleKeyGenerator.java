package com.config.compare.config;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Oracle数据库主键生成器
 * 用于Oracle数据库的主键ID生成
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class OracleKeyGenerator implements IdentifierGenerator {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseProperties databaseProperties;

    @Override
    public Number nextId(Object entity) {
        // 只有Oracle数据库才使用此生成器
        if (!"oracle".equalsIgnoreCase(databaseProperties.getType())) {
            return null;
        }

        String tableName = getTableName(entity);
        if (tableName == null) {
            log.warn("无法获取实体类对应的表名: {}", entity.getClass().getName());
            return null;
        }

        try {
            String sequenceName = "seq_" + tableName.toLowerCase();
            String sql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
            Long nextId = jdbcTemplate.queryForObject(sql, Long.class);
            log.debug("为表 {} 生成主键ID: {}", tableName, nextId);
            return nextId;
        } catch (Exception e) {
            log.error("生成Oracle主键ID失败，表名: {}", tableName, e);
            return null;
        }
    }

    /**
     * 根据实体类获取表名
     */
    private String getTableName(Object entity) {
        Class<?> entityClass = entity.getClass();
        
        // 尝试从类名推断表名
        String className = entityClass.getSimpleName();
        
        // 处理常见的实体类名到表名的映射
        switch (className) {
            case "SystemInfo":
                return "sys_system_info";
            case "ServerType":
                return "server_type";
            case "ServerInstance":
                return "server_instance";
            case "ConfigCategory":
                return "config_category";
            case "ConfigBaseline":
                return "config_baseline";
            case "BaselineVersionLog":
                return "baseline_version_log";
            case "CollectTypeExtension":
                return "collect_type_extension";
            case "CollectConfigParams":
                return "collect_config_params";
            case "CollectTemplate":
                return "collect_template";
            case "CollectTask":
                return "collect_task";
            case "CollectExecution":
                return "collect_execution";
            case "CollectResultEntity":
                return "collect_result";
            case "CompareTask":
                return "compare_task";
            case "CompareExecution":
                return "compare_execution";
            case "CompareResult":
                return "compare_result";
            case "DiffDetail":
                return "diff_detail";
            case "ReportTemplate":
                return "report_template";
            case "ReportRecord":
                return "report_record";
            default:
                // 默认将驼峰命名转换为下划线命名
                return camelToUnderscore(className);
        }
    }

    /**
     * 驼峰命名转下划线命名
     */
    private String camelToUnderscore(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}