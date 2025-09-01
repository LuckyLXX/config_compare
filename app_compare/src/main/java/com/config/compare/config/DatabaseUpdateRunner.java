package com.config.compare.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库更新运行器
 * 用于在应用启动时执行数据库结构更新
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-26
 */
@Slf4j
@Component
@Order(1)
public class DatabaseUpdateRunner implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        try {
            updateServerTypeTable();
            updateConfigCategoryTable();
        } catch (Exception e) {
            log.error("数据库更新失败", e);
        }
    }

    /**
     * 更新server_type表结构
     */
    private void updateServerTypeTable() {
        try {
            // 检查update_time字段是否存在
            String checkUpdateTimeSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'config_compare' AND TABLE_NAME = 'server_type' AND COLUMN_NAME = 'update_time'";
            
            Integer updateTimeExists = jdbcTemplate.queryForObject(checkUpdateTimeSql, Integer.class);
            
            if (updateTimeExists == null || updateTimeExists == 0) {
                log.info("添加server_type表的update_time字段...");
                String addUpdateTimeSql = "ALTER TABLE server_type " +
                        "ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' " +
                        "AFTER create_time";
                jdbcTemplate.execute(addUpdateTimeSql);
                log.info("成功添加update_time字段");
            }

            // 检查update_by字段是否存在
            String checkUpdateBySql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'config_compare' AND TABLE_NAME = 'server_type' AND COLUMN_NAME = 'update_by'";
            
            Integer updateByExists = jdbcTemplate.queryForObject(checkUpdateBySql, Integer.class);
            
            if (updateByExists == null || updateByExists == 0) {
                log.info("添加server_type表的update_by字段...");
                String addUpdateBySql = "ALTER TABLE server_type " +
                        "ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' " +
                        "AFTER update_time";
                jdbcTemplate.execute(addUpdateBySql);
                log.info("成功添加update_by字段");
            }

            log.info("server_type表结构检查完成");
            
        } catch (Exception e) {
            log.error("更新server_type表结构失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 更新config_category表结构
     */
    private void updateConfigCategoryTable() {
        try {
            // 检查update_time字段是否存在
            String checkUpdateTimeSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'config_compare' AND TABLE_NAME = 'config_category' AND COLUMN_NAME = 'update_time'";
            
            Integer updateTimeExists = jdbcTemplate.queryForObject(checkUpdateTimeSql, Integer.class);
            
            if (updateTimeExists == null || updateTimeExists == 0) {
                log.info("添加config_category表的update_time字段...");
                String addUpdateTimeSql = "ALTER TABLE config_category " +
                        "ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' " +
                        "AFTER create_time";
                jdbcTemplate.execute(addUpdateTimeSql);
                log.info("成功添加config_category.update_time字段");
            }

            // 检查update_by字段是否存在
            String checkUpdateBySql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'config_compare' AND TABLE_NAME = 'config_category' AND COLUMN_NAME = 'update_by'";
            
            Integer updateByExists = jdbcTemplate.queryForObject(checkUpdateBySql, Integer.class);
            
            if (updateByExists == null || updateByExists == 0) {
                log.info("添加config_category表的update_by字段...");
                String addUpdateBySql = "ALTER TABLE config_category " +
                        "ADD COLUMN update_by VARCHAR(100) COMMENT '更新人' " +
                        "AFTER update_time";
                jdbcTemplate.execute(addUpdateBySql);
                log.info("成功添加config_category.update_by字段");
            }

            log.info("config_category表结构检查完成");
            
        } catch (Exception e) {
            log.error("更新config_category表结构失败", e);
            // 不抛出异常，避免影响应用启动
        }
    }
}
