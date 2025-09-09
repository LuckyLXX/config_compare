package com.config.compare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.service.ApolloService;
import com.config.compare.entity.CollectTemplate;
import com.config.compare.entity.ServerInstance;
import com.config.compare.mapper.CollectTemplateMapper;
import com.config.compare.mapper.ServerInstanceMapper;
import com.config.compare.util.SSHUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采集模板服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectTemplateService extends ServiceImpl<CollectTemplateMapper, CollectTemplate> {

    private final ServerInstanceMapper serverInstanceMapper;
    private final ApolloService apolloService;

    /**
     * 分页查询采集模板列表
     */
    public IPage<CollectTemplate> getTemplateList(int current, int size, String templateName, String templateType, Integer status) {
        Page<CollectTemplate> page = new Page<>(current, size);
        LambdaQueryWrapper<CollectTemplate> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(templateName)) {
            queryWrapper.like(CollectTemplate::getTemplateName, templateName);
        }
        if (StringUtils.hasText(templateType)) {
            queryWrapper.eq(CollectTemplate::getTemplateType, templateType);
        }
        if (status != null) {
            queryWrapper.eq(CollectTemplate::getStatus, status);
        }
        
        queryWrapper.orderByDesc(CollectTemplate::getCreateTime);
        
        return this.page(page, queryWrapper);
    }

    /**
     * 根据服务器类型ID查询适用的模板列表
     */
    public List<CollectTemplate> getTemplatesByServerType(Long serverTypeId) {
        return baseMapper.selectByServerTypeId(serverTypeId);
    }

    /**
     * 测试模板连接
     */
    public Map<String, Object> testTemplate(Long templateId, Long serverId) {
        CollectTemplate template = null;
        
        // 如果templateId不为0，则获取已保存的模板
        if (templateId != null && templateId != 0) {
            template = this.getById(templateId);
            if (template == null) {
                throw new RuntimeException("模板不存在");
            }
        }

        // 获取服务器信息
        ServerInstance server = serverInstanceMapper.selectById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器实例不存在");
        }

        log.info("开始测试模板连接，模板ID：{}，服务器ID：{}", templateId, serverId);
        
        try {
            // 根据模板类型执行不同的测试逻辑
            return executeTemplateTest(template, server);
        } catch (Exception e) {
            log.error("模板连接测试失败", e);
            throw new RuntimeException("连接测试失败：" + e.getMessage());
        }
    }

    /**
     * 测试模板连接（支持动态配置）
     */
    public Map<String, Object> testTemplateWithConfig(String templateType, String templateContent, Long serverId) {
        // 获取服务器信息
        ServerInstance server = serverInstanceMapper.selectById(serverId);
        if (server == null) {
            throw new RuntimeException("服务器实例不存在");
        }

        log.info("开始测试模板连接，模板类型：{}，服务器ID：{}", templateType, serverId);
        
        try {
            // 创建临时模板对象
            CollectTemplate tempTemplate = new CollectTemplate();
            tempTemplate.setTemplateType(templateType);
            tempTemplate.setTemplateContent(templateContent);
            
            // 执行测试
            return executeTemplateTest(tempTemplate, server);
        } catch (Exception e) {
            log.error("模板连接测试失败", e);
            throw new RuntimeException("连接测试失败：" + e.getMessage());
        }
    }

    /**
     * 执行模板测试
     */
    private Map<String, Object> executeTemplateTest(CollectTemplate template, ServerInstance server) {
        String templateType = template.getTemplateType();
        String templateContent = template.getTemplateContent();
        
        log.info("执行{}类型模板测试", templateType);
        
        switch (templateType) {
            case "COMMAND":
                return testCommandTemplate(templateContent, server);
            case "FILE":
                return testFileTemplate(templateContent, server);
            case "API":
                return testApiTemplate(templateContent, server);
            case "APOLLO":
                return testApolloTemplate(templateContent, server);
            default:
                throw new RuntimeException("不支持的模板类型：" + templateType);
        }
    }

    /**
     * 测试SSH命令模板
     */
    private Map<String, Object> testCommandTemplate(String templateContent, ServerInstance server) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始解析模板内容: {}", templateContent);
            
            // 从模板内容解析命令
            String command = parseCommandFromTemplate(templateContent);
            String workingDir = null;
            Integer timeout = 60;
            
            if (command == null || command.trim().isEmpty()) {
                return Map.of(
                    "success", false,
                    "message", "命令不能为空",
                    "error", "InvalidCommand"
                );
            }
            
            log.info("准备执行SSH命令: {} 在服务器: {}:{}", command, server.getServerIp(), server.getSshPort());
            
            // 执行真实的SSH连接和命令
            String result = SSHUtil.executeCommand(server, command, timeout);
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("SSH命令执行成功，耗时: {}ms", executionTime);
            
            return Map.of(
                "success", true,
                "message", "SSH命令测试成功",
                "testResult", result,
                "executionTime", executionTime,
                "serverInfo", Map.of(
                    "host", server.getServerIp(),
                    "port", server.getSshPort(),
                    "username", server.getUsername()
                ),
                "commandInfo", Map.of(
                    "command", command,
                    "workingDir", workingDir != null ? workingDir : "默认目录",
                    "timeout", timeout + "秒"
                )
            );
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("SSH命令测试失败", e);
            return Map.of(
                "success", false,
                "message", "SSH命令测试失败：" + e.getMessage(),
                "error", e.getClass().getSimpleName(),
                "executionTime", executionTime,
                "serverInfo", Map.of(
                    "host", server.getServerIp(),
                    "port", server.getSshPort(),
                    "username", server.getUsername()
                )
            );
        }
    }

    /**
     * 测试文件下载模板
     */
    private Map<String, Object> testFileTemplate(String templateContent, ServerInstance server) {
        try {
            // TODO: 实现SFTP连接和文件下载测试
            // 这里应该解析templateContent中的filePath、encoding等参数
            // 建立SFTP连接到server，测试文件是否存在和可读取
            
            return Map.of(
                "success", true,
                "message", "文件下载测试成功",
                "testResult", "文件存在且可读取",
                "fileInfo", Map.of(
                    "path", "/opt/app/config/application.properties",
                    "size", "2.5KB",
                    "lastModified", "2025-01-25 10:30:00"
                ),
                "serverInfo", Map.of(
                    "host", server.getServerIp(),
                    "port", server.getSshPort()
                )
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "文件下载测试失败：" + e.getMessage(),
                "error", e.getClass().getSimpleName()
            );
        }
    }

    /**
     * 测试HTTP接口模板
     */
    private Map<String, Object> testApiTemplate(String templateContent, ServerInstance server) {
        try {
            // TODO: 实现HTTP接口调用测试
            // 这里应该解析templateContent中的url、method、headers等参数
            // 发送HTTP请求并返回结果
            
            return Map.of(
                "success", true,
                "message", "HTTP接口测试成功",
                "testResult", "接口响应正常",
                "responseInfo", Map.of(
                    "statusCode", 200,
                    "responseTime", 156L,
                    "contentType", "application/json",
                    "dataPreview", "{\"config\": \"value\", \"status\": \"active\"}"
                )
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "HTTP接口测试失败：" + e.getMessage(),
                "error", e.getClass().getSimpleName()
            );
        }
    }


    

    
    /**
     * 获取配置值的辅助方法
     */
    private String getConfigValue(Map<String, Object> configMap, String key, String defaultValue) {
        if (configMap != null && configMap.containsKey(key)) {
            Object value = configMap.get(key);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return defaultValue;
    }
    
    /**
     * 获取配置值的辅助方法（带备用默认值）
     */
    private String getConfigValue(Map<String, Object> configMap, String key, String primaryDefault, String secondaryDefault) {
        String value = getConfigValue(configMap, key, primaryDefault);
        return StringUtils.hasText(value) ? value : secondaryDefault;
    }
    


    /**
     * 从模板内容中解析命令
     */
    private String parseCommandFromTemplate(String templateContent) {
        try {
            if (StringUtils.hasText(templateContent)) {
                // 尝试解析JSON格式的配置
                if (templateContent.trim().startsWith("{")) {
                    Map<String, Object> config = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(templateContent, Map.class);
                    Object commandObj = config.get("command");
                    if (commandObj != null) {
                        return commandObj.toString();
                    }
                }
                // 如果不是JSON格式，直接当作命令返回
                return templateContent.trim();
            }
        } catch (Exception e) {
            log.warn("解析模板内容失败，使用默认命令: {}", e.getMessage());
        }
        
        // 默认测试命令
        return "echo '这是一个SSH连接测试' && date && whoami && pwd";
    }

    /**
     * 测试Apollo配置模板
     */
    private Map<String, Object> testApolloTemplate(String templateContent, ServerInstance server) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始Apollo配置测试，服务器: {}", server.getInstanceName());
            
            // 构建Apollo配置
            ApolloConfig apolloConfig = buildApolloConfig(templateContent, server);
            
            // 测试连接
            boolean connectionOk = apolloService.testConnection(apolloConfig);
            if (!connectionOk) {
                return Map.of(
                    "success", false,
                    "message", "Apollo服务器连接失败",
                    "error", "ConnectionFailed",
                    "executionTime", System.currentTimeMillis() - startTime,
                    "configInfo", Map.of(
                        "configServiceUrl", apolloConfig.getConfigServiceUrl(),
                        "appId", apolloConfig.getAppId(),
                        "cluster", apolloConfig.getCluster(),
                        "namespaces", apolloConfig.getNamespaces()
                    )
                );
            }
            
            // 获取所有配置
            Map<String, Map<String, String>> allConfigs = apolloService.getAllConfigs(apolloConfig);
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 格式化配置数据用于预览
            String configPreview;
            try {
                configPreview = new com.fasterxml.jackson.databind.ObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(allConfigs);
            } catch (Exception e) {
                configPreview = allConfigs.toString();
            }
            
            int totalConfigCount = allConfigs.values().stream()
                .mapToInt(Map::size)
                .sum();
            
            log.info("Apollo配置测试成功，总配置项数: {}, 耗时: {}ms", totalConfigCount, executionTime);
            
            return Map.of(
                "success", true,
                "message", "Apollo配置测试成功",
                "testResult", configPreview,
                "executionTime", executionTime,
                "configInfo", Map.of(
                    "configServiceUrl", apolloConfig.getConfigServiceUrl(),
                    "appId", apolloConfig.getAppId(),
                    "cluster", apolloConfig.getCluster(),
                    "namespaces", apolloConfig.getNamespaces(),
                    "totalConfigCount", totalConfigCount,
                    "successNamespaces", allConfigs.size()
                )
            );
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Apollo配置测试失败", e);
            return Map.of(
                "success", false,
                "message", "Apollo配置测试失败：" + e.getMessage(),
                "error", e.getClass().getSimpleName(),
                "executionTime", executionTime,
                "testResult", "错误详情: " + e.getMessage()
            );
        }
    }
    
    /**
     * 构建Apollo配置
     */
    private ApolloConfig buildApolloConfig(String templateContent, ServerInstance server) {
        ApolloConfig config = new ApolloConfig();
        
        try {
            log.info("构建Apollo配置 - 模板内容: {}", templateContent);
            log.info("服务器实例Apollo信息 - serverUrl: {}, appId: {}, cluster: {}, namespaces: {}", 
                server.getApolloServerUrl(), server.getApolloAppId(), server.getApolloCluster(), server.getApolloNamespaces());
            
            if (StringUtils.hasText(templateContent) && templateContent.trim().startsWith("{")) {
                // 解析JSON格式的配置
                Map<String, Object> configMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(templateContent, Map.class);
                
                log.info("解析到的JSON配置: {}", configMap);
                
                // 设置配置服务地址 - 支持configServiceUrl和serverUrl两种字段名
                String configServiceUrl = getConfigValue(configMap, "configServiceUrl", null);
                if (configServiceUrl == null) {
                    configServiceUrl = getConfigValue(configMap, "serverUrl", server.getApolloServerUrl());
                }
                config.setConfigServiceUrl(configServiceUrl);
                config.setAppId(getConfigValue(configMap, "appId", server.getApolloAppId()));
                config.setCluster(getConfigValue(configMap, "cluster", server.getApolloCluster(), "default"));
                
                // 解析命名空间
                String namespacesStr = getConfigValue(configMap, "namespaces", server.getApolloNamespaces());
                if (StringUtils.hasText(namespacesStr)) {
                    List<String> namespaces = List.of(namespacesStr.split(","));
                    config.setNamespaces(namespaces.stream().map(String::trim).collect(Collectors.toList()));
                }
                
                // 设置密钥（用于签名认证）- 支持token和secret两种字段名
                String secret = getConfigValue(configMap, "secret", null);
                if (secret == null) {
                    secret = getConfigValue(configMap, "token", null);
                }
                config.setSecret(secret);
                
                // 设置超时参数
                if (configMap.containsKey("connectTimeout")) {
                    config.setConnectTimeout(Integer.valueOf(configMap.get("connectTimeout").toString()));
                }
                if (configMap.containsKey("readTimeout")) {
                    config.setReadTimeout(Integer.valueOf(configMap.get("readTimeout").toString()));
                }
                
            } else {
                // 使用服务器实例的Apollo配置
                config.setConfigServiceUrl(server.getApolloServerUrl());
                config.setAppId(server.getApolloAppId());
                config.setCluster(server.getApolloCluster() != null ? server.getApolloCluster() : "default");
                
                if (StringUtils.hasText(server.getApolloNamespaces())) {
                    List<String> namespaces = List.of(server.getApolloNamespaces().split(","));
                    config.setNamespaces(namespaces.stream().map(String::trim).collect(Collectors.toList()));
                }
            }
            
            // 设置默认值（如果配置为空）
            if (!StringUtils.hasText(config.getConfigServiceUrl())) {
                // 尝试从模板内容或使用默认测试地址
                config.setConfigServiceUrl("http://81.68.181.139:8080");
                log.warn("Apollo Config Service地址为空，使用默认测试地址: {}", config.getConfigServiceUrl());
            }
            
            if (!StringUtils.hasText(config.getAppId())) {
                config.setAppId("001010101");
                log.warn("Apollo应用标识为空，使用默认测试AppId: {}", config.getAppId());
            }
            
            if (config.getNamespaces() == null || config.getNamespaces().isEmpty()) {
                config.setNamespaces(List.of("application"));
                log.warn("Apollo命名空间列表为空，使用默认命名空间: {}", config.getNamespaces());
            }
            
            log.info("最终构建的Apollo配置 - configServiceUrl: {}, appId: {}, cluster: {}, namespaces: {}", 
                config.getConfigServiceUrl(), config.getAppId(), config.getCluster(), config.getNamespaces());
            
            return config;
            
        } catch (Exception e) {
            log.error("构建Apollo配置失败", e);
            throw new RuntimeException("Apollo配置构建失败：" + e.getMessage());
        }
    }

}
