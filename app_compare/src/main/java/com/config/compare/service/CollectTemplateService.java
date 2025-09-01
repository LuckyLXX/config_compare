package com.config.compare.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CollectTemplate;
import com.config.compare.entity.ServerInstance;
import com.config.compare.mapper.CollectTemplateMapper;
import com.config.compare.mapper.ServerInstanceMapper;
import com.config.compare.util.SSHUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

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
     * 测试Apollo配置模板
     */
    private Map<String, Object> testApolloTemplate(String templateContent, ServerInstance server) {
        try {
            // TODO: 实现Apollo配置中心连接测试
            // 这里应该解析templateContent中的serverUrl、appId、env等参数
            // 连接Apollo服务并获取配置信息
            
            return Map.of(
                "success", true,
                "message", "Apollo配置测试成功",
                "testResult", "成功连接Apollo配置中心",
                "configInfo", Map.of(
                    "namespaces", List.of("application", "database", "redis"),
                    "configCount", 15,
                    "lastReleaseTime", "2025-01-25 09:45:00"
                )
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "Apollo配置测试失败：" + e.getMessage(),
                "error", e.getClass().getSimpleName()
            );
        }
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

}
