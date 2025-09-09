package com.config.compare.collect.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.service.ApolloService;
import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Apollo配置中心采集处理器 - 简化版
 * 
 * @author system
 * @version 2.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApolloCollectHandler extends AbstractCollectHandler {

    private final ApolloService apolloService;

    @Override
    public String getTypeCode() {
        return "APOLLO";
    }

    @Override
    public String getTypeName() {
        return "Apollo配置中心";
    }

    @Override
    public String getDescription() {
        return "从Apollo配置中心采集配置信息，直接使用HTTP请求";
    }

    @Override
    public boolean testConnection(CollectContext context) {
        try {
            ApolloConfig apolloConfig = buildApolloConfig(context);
            return apolloService.testConnection(apolloConfig);
        } catch (Exception e) {
            log.error("Apollo连接测试失败", e);
            return false;
        }
    }

    @Override
    protected CollectResult doCollect(CollectContext context) {
        log.info("开始Apollo配置采集，服务器实例: {}, 采集项: {}", 
                 context.getServerInstance().getInstanceName(), 
                 context.getCollectItemName());

        try {
            // 构建Apollo配置
            ApolloConfig apolloConfig = buildApolloConfig(context);
            
            // 测试连接
            boolean connectionOk = apolloService.testConnection(apolloConfig);
            if (!connectionOk) {
                return CollectResult.error("Apollo服务器连接失败")
                        .setApiEndpoint(apolloConfig.getConfigServiceUrl());
            }

            // 采集所有命名空间的配置
            Map<String, Map<String, String>> allConfigs = apolloService.getAllConfigs(apolloConfig);
            
            // 统计信息
            int totalConfigCount = allConfigs.values().stream()
                    .mapToInt(Map::size)
                    .sum();
            
            // 添加采集信息
            Map<String, Object> result = new java.util.HashMap<>(allConfigs);
            result.put("_collectInfo", Map.of(
                "configServiceUrl", apolloConfig.getConfigServiceUrl(),
                "appId", apolloConfig.getAppId(),
                "cluster", apolloConfig.getCluster(),
                "totalNamespaces", allConfigs.size(),
                "totalConfigCount", totalConfigCount,
                "collectTime", java.time.LocalDateTime.now().toString()
            ));

            String content = JSONUtil.toJsonPrettyStr(result);
            log.info("Apollo配置采集完成，总命名空间数: {}, 总配置项: {}", 
                    allConfigs.size(), totalConfigCount);

            return CollectResult.success(content)
                    .setFilePath("apollo://" + apolloConfig.getAppId())
                    .setApiEndpoint(apolloConfig.getConfigServiceUrl())
                    .setNamespace(String.join(",", apolloConfig.getNamespaces()));
                    
        } catch (Exception e) {
            log.error("Apollo配置采集失败", e);
            return CollectResult.error("采集失败: " + e.getMessage());
        }
    }

    /**
     * 从采集上下文构建Apollo配置
     */
    private ApolloConfig buildApolloConfig(CollectContext context) {
        var serverInstance = context.getServerInstance();
        var configParams = context.getConfigParams();

        ApolloConfig config = new ApolloConfig();
        
        // 优先使用配置参数，其次使用服务器实例配置
        config.setConfigServiceUrl(getConfigValue(configParams, "configServiceUrl", serverInstance.getApolloServerUrl()));
        config.setAppId(getConfigValue(configParams, "appId", serverInstance.getApolloAppId()));
        config.setCluster(getConfigValue(configParams, "cluster", serverInstance.getApolloCluster(), "default"));

        // 解析命名空间列表
        String namespacesStr = getConfigValue(configParams, "namespaces", serverInstance.getApolloNamespaces());
        if (StrUtil.isNotBlank(namespacesStr)) {
            List<String> namespaces = StrUtil.split(namespacesStr, ',', true, true);
            config.setNamespaces(namespaces);
        } else {
            // 默认命名空间
            config.setNamespaces(List.of("application"));
        }

        // 设置超时参数
        if (configParams != null) {
            Integer connectTimeout = (Integer) configParams.get("connectTimeout");
            Integer readTimeout = (Integer) configParams.get("readTimeout");
            
            if (connectTimeout != null) config.setConnectTimeout(connectTimeout);
            if (readTimeout != null) config.setReadTimeout(readTimeout);
        }

        // 验证必需参数
        validateApolloConfig(config);

        return config;
    }

    /**
     * 获取配置值的辅助方法
     */
    private String getConfigValue(Map<String, Object> configParams, String key, String defaultValue) {
        if (configParams != null && configParams.containsKey(key)) {
            return String.valueOf(configParams.get(key));
        }
        return defaultValue;
    }

    /**
     * 获取配置值的辅助方法（带备用默认值）
     */
    private String getConfigValue(Map<String, Object> configParams, String key, String primaryDefault, String secondaryDefault) {
        String value = getConfigValue(configParams, key, primaryDefault);
        return StrUtil.isNotBlank(value) ? value : secondaryDefault;
    }

    /**
     * 验证Apollo配置
     */
    private void validateApolloConfig(ApolloConfig config) {
        if (StrUtil.isBlank(config.getConfigServiceUrl())) {
            throw new IllegalArgumentException("Apollo Config Service地址不能为空");
        }
        if (StrUtil.isBlank(config.getAppId())) {
            throw new IllegalArgumentException("Apollo应用标识不能为空");
        }
        if (config.getNamespaces() == null || config.getNamespaces().isEmpty()) {
            throw new IllegalArgumentException("Apollo命名空间列表不能为空");
        }
    }

    @Override
    public String getConfigSchema() {
        return "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"configServiceUrl\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"Apollo Config Service地址\",\n" +
            "      \"example\": \"http://81.68.181.139:8080\"\n" +
            "    },\n" +
            "    \"appId\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"应用标识\",\n" +
            "      \"example\": \"001010101\"\n" +
            "    },\n" +
            "    \"cluster\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"集群名称\",\n" +
            "      \"default\": \"default\"\n" +
            "    },\n" +
            "    \"namespaces\": {\n" +
            "      \"type\": \"string\",\n" +
            "      \"description\": \"命名空间列表，逗号分隔\",\n" +
            "      \"example\": \"application,database,redis\"\n" +
            "    },\n" +
            "    \"connectTimeout\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"description\": \"连接超时时间（毫秒）\",\n" +
            "      \"default\": 5000\n" +
            "    },\n" +
            "    \"readTimeout\": {\n" +
            "      \"type\": \"integer\",\n" +
            "      \"description\": \"读取超时时间（毫秒）\",\n" +
            "      \"default\": 10000\n" +
            "    }\n" +
            "  },\n" +
            "  \"required\": [\"configServiceUrl\", \"appId\", \"namespaces\"],\n" +
            "  \"description\": \"直接使用HTTP请求获取Apollo配置，无需Token或管理员权限\"\n" +
            "}";
    }
}