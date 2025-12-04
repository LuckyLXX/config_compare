package com.config.compare.config;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 动态数据源配置类
 * 根据配置自动选择MySQL或Oracle数据库
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Configuration
public class DynamicDataSourceConfig {

    @Autowired
    private DatabaseProperties databaseProperties;

    /**
     * 创建动态数据源
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.druid")
    public DataSource dynamicDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        
        // 根据配置选择数据库类型
        if ("oracle".equalsIgnoreCase(databaseProperties.getType())) {
            configureOracleDataSource(dataSource);
        } else {
            configureMySQLDataSource(dataSource);
        }
        
        return dataSource;
    }

    /**
     * 配置Oracle数据源
     */
    private void configureOracleDataSource(DruidDataSource dataSource) {
        DatabaseProperties.OracleConfig oracleConfig = databaseProperties.getOracle();
        
        dataSource.setDriverClassName(oracleConfig.getDriverClassName());
        dataSource.setUrl(oracleConfig.getUrl());
        dataSource.setUsername(oracleConfig.getUsername());
        dataSource.setPassword(oracleConfig.getPassword());
        
        // Oracle特定配置
        dataSource.setValidationQuery("SELECT 1 FROM DUAL");
        dataSource.setDefaultAutoCommit(false);
        
        log.info("配置Oracle数据源: {}", oracleConfig.getUrl());
    }

    /**
     * 配置MySQL数据源
     */
    private void configureMySQLDataSource(DruidDataSource dataSource) {
        DatabaseProperties.MysqlConfig mysqlConfig = databaseProperties.getMysql();
        
        dataSource.setDriverClassName(mysqlConfig.getDriverClassName());
        dataSource.setUrl(mysqlConfig.getUrl());
        dataSource.setUsername(mysqlConfig.getUsername());
        dataSource.setPassword(mysqlConfig.getPassword());
        
        // MySQL特定配置
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setDefaultAutoCommit(true);
        
        log.info("配置MySQL数据源: {}", mysqlConfig.getUrl());
    }
}