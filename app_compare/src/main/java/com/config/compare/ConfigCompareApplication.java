package com.config.compare;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 配置比对系统主应用类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.config.compare.mapper")
public class ConfigCompareApplication {

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "true");
        SpringApplication.run(ConfigCompareApplication.class, args);
        System.out.println("========================================");
        System.out.println("  配置比对系统启动成功！");
        System.out.println("  接口文档地址: http://localhost:8080/api/swagger-ui/index.html");
        System.out.println("  数据库监控: http://localhost:8080/api/druid/");
        System.out.println("========================================");
    }
}