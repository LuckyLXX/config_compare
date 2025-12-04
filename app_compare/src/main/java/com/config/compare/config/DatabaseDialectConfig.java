package com.config.compare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.PostConstruct;

/**
 * 数据库方言配置类
 * 处理MySQL和Oracle之间的语法差异
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Configuration
public class DatabaseDialectConfig {

    @Autowired
    private DatabaseProperties databaseProperties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 数据库类型枚举
     */
    public enum DatabaseType {
        MYSQL, ORACLE
    }

    /**
     * 获取当前数据库类型
     */
    public DatabaseType getCurrentDatabaseType() {
        String type = databaseProperties.getType();
        if ("oracle".equalsIgnoreCase(type)) {
            return DatabaseType.ORACLE;
        } else {
            return DatabaseType.MYSQL;
        }
    }

    /**
     * 获取分页SQL
     */
    public String getPageSql(String originalSql, int offset, int limit) {
        DatabaseType dbType = getCurrentDatabaseType();
        
        switch (dbType) {
            case ORACLE:
                return getOraclePageSql(originalSql, offset, limit);
            case MYSQL:
            default:
                return getMySQLPageSql(originalSql, offset, limit);
        }
    }

    /**
     * 获取MySQL分页SQL
     */
    private String getMySQLPageSql(String originalSql, int offset, int limit) {
        return originalSql + " LIMIT " + offset + ", " + limit;
    }

    /**
     * 获取Oracle分页SQL
     */
    private String getOraclePageSql(String originalSql, int offset, int limit) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM (SELECT a.*, ROWNUM rn FROM (");
        sql.append(originalSql);
        sql.append(") a WHERE ROWNUM <= ");
        sql.append(offset + limit);
        sql.append(") WHERE rn > ");
        sql.append(offset);
        return sql.toString();
    }

    /**
     * 获取字符串聚合函数
     */
    public String getGroupConcatFunction(String columnName, String separator) {
        DatabaseType dbType = getCurrentDatabaseType();
        
        switch (dbType) {
            case ORACLE:
                return "LISTAGG(" + columnName + ", '" + separator + "') WITHIN GROUP (ORDER BY " + columnName + ")";
            case MYSQL:
            default:
                return "GROUP_CONCAT(" + columnName + " SEPARATOR '" + separator + "')";
        }
    }

    /**
     * 获取当前时间函数
     */
    public String getCurrentTimeFunction() {
        DatabaseType dbType = getCurrentDatabaseType();
        
        switch (dbType) {
            case ORACLE:
                return "CURRENT_TIMESTAMP";
            case MYSQL:
            default:
                return "NOW()";
        }
    }

    /**
     * 获取空值处理函数
     */
    public String getIfNullFunction(String expression, String defaultValue) {
        DatabaseType dbType = getCurrentDatabaseType();
        
        switch (dbType) {
            case ORACLE:
                return "NVL(" + expression + ", " + defaultValue + ")";
            case MYSQL:
            default:
                return "IFNULL(" + expression + ", " + defaultValue + ")";
        }
    }

    /**
     * 初始化数据库方言
     */
    @PostConstruct
    public void initDialect() {
        DatabaseType dbType = getCurrentDatabaseType();
        log.info("当前数据库类型: {}", dbType);
        
        // 测试数据库连接
        try {
            String testSql = getCurrentDatabaseType() == DatabaseType.ORACLE 
                ? "SELECT 1 FROM DUAL" 
                : "SELECT 1";
            jdbcTemplate.queryForObject(testSql, Integer.class);
            log.info("数据库连接测试成功");
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
        }
    }
}