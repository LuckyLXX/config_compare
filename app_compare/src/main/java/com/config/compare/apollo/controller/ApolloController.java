package com.config.compare.apollo.controller;

import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.service.ApolloService;
import com.config.compare.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Apollo配置中心管理控制器 - 简化版
 * 
 * @author system
 * @version 2.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/apollo")
@RequiredArgsConstructor
@Tag(name = "Apollo配置中心管理", description = "Apollo配置中心相关操作，直接使用HTTP请求")
public class ApolloController {

    private final ApolloService apolloService;

    @PostMapping("/test-connection")
    @Operation(summary = "测试Apollo连接", description = "测试Apollo Config Service连接是否正常")
    public Result<Boolean> testConnection(@Validated @RequestBody ApolloConfig config) {
        log.info("测试Apollo连接: {}", config.getConfigServiceUrl());
        
        try {
            boolean connected = apolloService.testConnection(config);
            return Result.success(connected ? "连接成功" : "连接失败", connected);
        } catch (Exception e) {
            log.error("测试Apollo连接失败", e);
            return Result.error("连接测试失败: " + e.getMessage());
        }
    }

    @PostMapping("/namespace/configs")
    @Operation(summary = "获取命名空间配置", description = "获取指定命名空间的配置详情")
    public Result<Map<String, String>> getNamespaceConfigs(
            @Validated @RequestBody ApolloConfig config,
            @Parameter(description = "命名空间名称") @RequestParam String namespace) {
        log.info("获取Apollo命名空间配置: appId={}, namespace={}", config.getAppId(), namespace);
        
        try {
            Map<String, String> configs = apolloService.getNamespaceConfigs(config, namespace);
            return Result.success(configs);
        } catch (Exception e) {
            log.error("获取命名空间配置失败", e);
            return Result.error("获取命名空间配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/all-configs")
    @Operation(summary = "获取所有配置", description = "获取指定应用的所有命名空间配置")
    public Result<Map<String, Map<String, String>>> getAllConfigs(@Validated @RequestBody ApolloConfig config) {
        log.info("获取Apollo所有配置: appId={}, namespaces={}", config.getAppId(), config.getNamespaces());
        
        try {
            Map<String, Map<String, String>> allConfigs = apolloService.getAllConfigs(config);
            return Result.success(allConfigs);
        } catch (Exception e) {
            log.error("获取所有配置失败", e);
            return Result.error("获取所有配置失败: " + e.getMessage());
        }
    }

    @GetMapping("/info")
    @Operation(summary = "获取Apollo采集器信息", description = "获取Apollo采集器的功能信息")
    public Result<Object> getApolloInfo() {
        return Result.success("Apollo采集器信息", Map.of(
            "version", "2.0.0",
            "type", "简化版",
            "features", java.util.List.of(
                "直接HTTP请求访问",
                "无需Token或管理员权限",
                "支持多命名空间采集",
                "简单易用"
            ),
            "urlFormat", "http://config-service:8080/configs/{appId}/{cluster}/{namespace}",
            "example", "http://81.68.181.139:8080/configs/001010101/default/application",
            "description", "直接使用HTTP请求获取Apollo配置，无需复杂的认证流程"
        ));
    }
}