package com.config.compare.apollo.controller;

import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.model.ApolloNamespace;
import com.config.compare.apollo.service.ApolloService;
import com.config.compare.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Apollo配置中心管理控制器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/apollo")
@RequiredArgsConstructor
@Tag(name = "Apollo配置中心管理", description = "Apollo配置中心相关操作")
public class ApolloController {

    private final ApolloService apolloService;

    @PostMapping("/test-connection")
    @Operation(summary = "测试Apollo连接", description = "测试Apollo服务器连接是否正常")
    public Result<Boolean> testConnection(@Validated @RequestBody ApolloConfig config) {
        log.info("测试Apollo连接: {}", config.getServerUrl());
        
        try {
            boolean connected = apolloService.testConnection(config);
            return Result.success(connected ? "连接成功" : "连接失败", connected);
        } catch (Exception e) {
            log.error("测试Apollo连接失败", e);
            return Result.error("连接测试失败: " + e.getMessage());
        }
    }

    @PostMapping("/namespaces")
    @Operation(summary = "获取命名空间列表", description = "获取指定应用的所有命名空间")
    public Result<List<String>> getNamespaces(@Validated @RequestBody ApolloConfig config) {
        log.info("获取Apollo命名空间列表: appId={}, env={}", config.getAppId(), config.getEnv());
        
        try {
            List<String> namespaces = apolloService.getNamespaces(config);
            return Result.success(namespaces);
        } catch (Exception e) {
            log.error("获取命名空间列表失败", e);
            return Result.error("获取命名空间列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/namespace/configs")
    @Operation(summary = "获取命名空间配置", description = "获取指定命名空间的配置详情")
    public Result<ApolloNamespace> getNamespaceConfigs(
            @Validated @RequestBody ApolloConfig config,
            @Parameter(description = "命名空间名称") @RequestParam String namespace) {
        log.info("获取Apollo命名空间配置: appId={}, namespace={}", config.getAppId(), namespace);
        
        try {
            ApolloNamespace namespaceData = apolloService.getNamespaceConfigs(config, namespace);
            return Result.success(namespaceData);
        } catch (Exception e) {
            log.error("获取命名空间配置失败", e);
            return Result.error("获取命名空间配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/namespace/published")
    @Operation(summary = "获取发布配置", description = "获取指定命名空间的已发布配置")
    public Result<String> getPublishedConfigs(
            @Validated @RequestBody ApolloConfig config,
            @Parameter(description = "命名空间名称") @RequestParam String namespace) {
        log.info("获取Apollo发布配置: appId={}, namespace={}", config.getAppId(), namespace);
        
        try {
            String publishedConfigs = apolloService.getPublishedConfigs(config, namespace);
            return Result.success(publishedConfigs);
        } catch (Exception e) {
            log.error("获取发布配置失败", e);
            return Result.error("获取发布配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/preview")
    @Operation(summary = "预览Apollo采集结果", description = "预览指定Apollo配置的所有命名空间配置")
    public Result<List<ApolloNamespace>> previewConfigs(@Validated @RequestBody ApolloConfig config) {
        log.info("预览Apollo配置: appId={}, namespaces={}", config.getAppId(), config.getNamespaces());
        
        try {
            List<ApolloNamespace> preview = apolloService.previewConfigs(config);
            return Result.success(preview);
        } catch (Exception e) {
            log.error("预览Apollo配置失败", e);
            return Result.error("预览配置失败: " + e.getMessage());
        }
    }
}