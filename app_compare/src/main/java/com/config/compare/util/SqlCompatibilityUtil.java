package com.config.compare.util;

import com.config.compare.config.DatabaseDialectConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SQL语法兼容性工具类
 * 处理MySQL和Oracle之间的SQL语法差异
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class SqlCompatibilityUtil {

    @Autowired
    private DatabaseDialectConfig databaseDialectConfig;

    /**
     * 处理分页SQL的兼容性
     * 将MySQL的LIMIT语法转换为Oracle兼容的语法
     */
    public String handlePageSql(String originalSql, int offset, int limit) {
        if (originalSql == null || originalSql.trim().isEmpty()) {
            return originalSql;
        }

        DatabaseDialectConfig.DatabaseType dbType = databaseDialectConfig.getCurrentDatabaseType();
        
        if (dbType == DatabaseDialectConfig.DatabaseType.ORACLE) {
            return databaseDialectConfig.getPageSql(originalSql, offset, limit);
        } else {
            // MySQL或其他数据库，直接使用LIMIT语法
            return originalSql + " LIMIT " + offset + ", " + limit;
        }
    }

    /**
     * 处理GROUP_CONCAT函数的兼容性
     * 将MySQL的GROUP_CONCAT转换为Oracle的LISTAGG
     */
    public String handleGroupConcat(String columnName, String separator) {
        return databaseDialectConfig.getGroupConcatFunction(columnName, separator);
    }

    /**
     * 处理IFNULL函数的兼容性
     * 将MySQL的IFNULL转换为Oracle的NVL
     */
    public String handleIfNull(String expression, String defaultValue) {
        return databaseDialectConfig.getIfNullFunction(expression, defaultValue);
    }

    /**
     * 处理时间函数的兼容性
     * 将MySQL的NOW()转换为Oracle的CURRENT_TIMESTAMP
     */
    public String handleCurrentTime() {
        return databaseDialectConfig.getCurrentTimeFunction();
    }

    /**
     * 处理MyBatis Plus的last方法中的LIMIT语法
     * 替换所有使用LIMIT的地方
     */
    public String convertMyBatisLastSql(String lastSql) {
        if (lastSql == null || lastSql.trim().isEmpty()) {
            return lastSql;
        }

        DatabaseDialectConfig.DatabaseType dbType = databaseDialectConfig.getCurrentDatabaseType();
        
        if (dbType == DatabaseDialectConfig.DatabaseType.ORACLE) {
            // 处理Oracle分页
            if (lastSql.toUpperCase().contains("LIMIT")) {
                return convertLimitToOracle(lastSql);
            }
        }
        
        return lastSql;
    }

    /**
     * 将LIMIT语法转换为Oracle语法
     */
    private String convertLimitToOracle(String limitSql) {
        try {
            String upperSql = limitSql.toUpperCase();
            int limitIndex = upperSql.indexOf("LIMIT");
            
            if (limitIndex == -1) {
                return limitSql;
            }
            
            String limitPart = limitSql.substring(limitIndex + 5).trim();
            String[] limitParams = limitPart.split(",");
            
            if (limitParams.length == 1) {
                // 只有LIMIT n的情况
                int limitValue = Integer.parseInt(limitParams[0].trim());
                return "ROWNUM <= " + limitValue;
            } else if (limitParams.length == 2) {
                // LIMIT offset, n的情况
                int offset = Integer.parseInt(limitParams[0].trim());
                int limitValue = Integer.parseInt(limitParams[1].trim());
                return "ROWNUM > " + offset + " AND ROWNUM <= " + (offset + limitValue);
            }
        } catch (Exception e) {
            log.error("转换LIMIT语法失败: {}", limitSql, e);
        }
        
        return limitSql;
    }

    /**
     * 处理字符串长度函数的兼容性
     * MySQL使用LENGTH，Oracle也使用LENGTH，但有些情况下可能需要特殊处理
     */
    public String handleLengthFunction(String expression) {
        DatabaseDialectConfig.DatabaseType dbType = databaseDialectConfig.getCurrentDatabaseType();
        
        if (dbType == DatabaseDialectConfig.DatabaseType.ORACLE) {
            // Oracle对于中文字符可能需要使用LENGTHB处理字节长度
            return "LENGTH(" + expression + ")";
        } else {
            return "LENGTH(" + expression + ")";
        }
    }

    /**
     * 处理字符串连接函数的兼容性
     * MySQL使用CONCAT，Oracle也支持CONCAT但只能连接两个字符串
     */
    public String handleConcatFunction(String... expressions) {
        DatabaseDialectConfig.DatabaseType dbType = databaseDialectConfig.getCurrentDatabaseType();
        
        if (dbType == DatabaseDialectConfig.DatabaseType.ORACLE) {
            if (expressions.length == 2) {
                return "CONCAT(" + expressions[0] + ", " + expressions[1] + ")";
            } else {
                // Oracle连接多个字符串需要使用||操作符
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < expressions.length; i++) {
                    if (i > 0) {
                        sb.append(" || ");
                    }
                    sb.append(expressions[i]);
                }
                return sb.toString();
            }
        } else {
            // MySQL支持多个参数的CONCAT
            StringBuilder sb = new StringBuilder("CONCAT(");
            for (int i = 0; i < expressions.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(expressions[i]);
            }
            sb.append(")");
            return sb.toString();
        }
    }
}