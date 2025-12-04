package com.config.compare.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 数据源配置类
 * 支持MySQL和Oracle数据库的动态切换
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Configuration
public class DataSourceConfig {

    /**
     * JdbcTemplate配置
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}