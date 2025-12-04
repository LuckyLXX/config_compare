package com.config.compare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 数据库配置属性类
 * 支持MySQL和Oracle数据库的配置管理
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@Component
@ConfigurationProperties(prefix = "database")
public class DatabaseProperties {

    /**
     * 数据库类型：mysql/oracle
     */
    private String type = "mysql";

    /**
     * MySQL配置
     */
    private MysqlConfig mysql = new MysqlConfig();

    /**
     * Oracle配置
     */
    private OracleConfig oracle = new OracleConfig();

    @Data
    public static class MysqlConfig {
        private String driverClassName = "com.mysql.cj.jdbc.Driver";
        private String url;
        private String username;
        private String password;
    }

    @Data
    public static class OracleConfig {
        private String driverClassName = "oracle.jdbc.OracleDriver";
        private String url;
        private String username;
        private String password;
    }
}