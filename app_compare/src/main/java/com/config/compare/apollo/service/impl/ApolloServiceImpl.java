package com.config.compare.apollo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.service.ApolloService;
import com.config.compare.apollo.util.ApolloSignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Apollo服务实现类 - 简化版，直接使用HTTP请求
 * 
 * @author system
 * @version 2.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class ApolloServiceImpl implements ApolloService {

    @Override
    public boolean testConnection(ApolloConfig config) {
        try {
            // 使用application命名空间测试连接
            String testUrl = buildConfigUrl(config, "application");
            log.info("测试Apollo连接: {}", testUrl);
            
            HttpResponse response = createAuthenticatedRequest(testUrl, config).execute();
            
            boolean success = response.isOk();
            log.info("Apollo连接测试结果: {}, 状态码: {}", success ? "成功" : "失败", response.getStatus());
            
            return success;
        } catch (Exception e) {
            log.error("Apollo连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Map<String, String> getNamespaceConfigs(ApolloConfig config, String namespace) {
        try {
            String url = buildConfigUrl(config, namespace);
            log.info("获取Apollo配置: {}", url);
            
            HttpResponse response = createAuthenticatedRequest(url, config).execute();
            
            if (!response.isOk()) {
                log.error("获取Apollo配置失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                throw new RuntimeException("获取Apollo配置失败，状态码: " + response.getStatus());
            }
            
            String responseBody = response.body();
            log.debug("Apollo响应内容: {}", responseBody);
            
            // 解析响应JSON
            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
            Map<String, String> configs = new HashMap<>();
            
            // 检查是否有configurations字段
            if (jsonResponse.containsKey("configurations")) {
                JSONObject configurations = jsonResponse.getJSONObject("configurations");
                for (String key : configurations.keySet()) {
                    configs.put(key, configurations.getStr(key));
                }
            } else {
                // 直接解析为配置项（某些Apollo版本的响应格式）
                for (String key : jsonResponse.keySet()) {
                    // 跳过元数据字段
                    if (!"appId".equals(key) && !"cluster".equals(key) && !"namespaceName".equals(key) 
                        && !"releaseKey".equals(key)) {
                        configs.put(key, jsonResponse.getStr(key));
                    }
                }
            }
            
            log.info("成功获取命名空间 {} 的 {} 个配置项", namespace, configs.size());
            return configs;
            
        } catch (Exception e) {
            log.error("获取Apollo命名空间 {} 配置失败: {}", namespace, e.getMessage());
            throw new RuntimeException("获取Apollo配置失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Map<String, String>> getAllConfigs(ApolloConfig config) {
        Map<String, Map<String, String>> allConfigs = new HashMap<>();
        
        // 如果没有指定命名空间，默认使用application
        if (config.getNamespaces() == null || config.getNamespaces().isEmpty()) {
            config.setNamespaces(java.util.List.of("application"));
        }
        
        for (String namespace : config.getNamespaces()) {
            try {
                Map<String, String> namespaceConfigs = getNamespaceConfigs(config, namespace);
                allConfigs.put(namespace, namespaceConfigs);
            } catch (Exception e) {
                log.error("获取命名空间 {} 配置失败: {}", namespace, e.getMessage());
                // 记录错误信息
                Map<String, String> errorInfo = new HashMap<>();
                errorInfo.put("error", e.getMessage());
                allConfigs.put(namespace, errorInfo);
            }
        }
        
        return allConfigs;
    }

    /**
     * 构建配置获取URL
     * URL格式: http://config-service:8080/configs/{appId}/{cluster}/{namespace}
     */
    private String buildConfigUrl(ApolloConfig config, String namespace) {
        String baseUrl = config.getConfigServiceUrl();
        
        // 确保URL以/结尾
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        // 构建完整URL
        return String.format("%sconfigs/%s/%s/%s", 
                baseUrl, 
                config.getAppId(), 
                config.getCluster(), 
                namespace);
    }
    
    /**
     * 创建带认证的HTTP请求
     */
    private HttpRequest createAuthenticatedRequest(String url, ApolloConfig config) {
        HttpRequest request = HttpRequest.get(url)
                .timeout(config.getConnectTimeout());
        
        // 如果配置了密钥，添加签名认证头部
        if (StringUtils.hasText(config.getSecret())) {
            try {
                Map<String, String> authHeaders = ApolloSignatureUtil.buildHttpHeaders(url, config.getAppId(), config.getSecret());
                log.info("Apollo认证头部信息:");
                for (Map.Entry<String, String> header : authHeaders.entrySet()) {
                    request.header(header.getKey(), header.getValue());
                    log.info("  {}: {}", header.getKey(), header.getValue());
                }
                log.info("已添加Apollo签名认证头部，AppId: {}, Secret: {}***", config.getAppId(), 
                    config.getSecret().length() > 3 ? config.getSecret().substring(0, 3) : "***");
            } catch (Exception e) {
                log.error("添加Apollo签名认证失败: {}", e.getMessage(), e);
            }
        } else {
            log.info("未配置Apollo密钥，使用无认证请求");
        }
        
        return request;
    }
}