package com.config.compare.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HTTP连接工具类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-26
 */
@Slf4j
public class HttpConnectionUtil {

    private static final int DEFAULT_TIMEOUT = 10000; // 10秒超时
    private static final int DEFAULT_READ_TIMEOUT = 15000; // 15秒读取超时

    /**
     * 测试HTTP连接
     * 
     * @param url 目标URL
     * @return 连接测试结果
     */
    public static boolean testHttpConnection(String url) {
        if (!StringUtils.hasText(url)) {
            log.warn("HTTP连接URL为空");
            return false;
        }

        try {
            // 确保URL格式正确
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "http://" + url;
            }

            URL targetUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
            
            // 设置连接参数
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(DEFAULT_TIMEOUT);
            connection.setReadTimeout(DEFAULT_READ_TIMEOUT);
            connection.setInstanceFollowRedirects(true);
            
            // 设置请求头
            connection.setRequestProperty("User-Agent", "ConfigCompare/1.0");
            connection.setRequestProperty("Accept", "*/*");
            
            log.info("正在测试HTTP连接: {}", url);
            
            // 尝试连接
            int responseCode = connection.getResponseCode();
            
            // 2xx和3xx状态码都认为是成功的
            if (responseCode >= 200 && responseCode < 400) {
                log.info("HTTP连接测试成功: {} (响应码: {})", url, responseCode);
                return true;
            } else if (responseCode == 401 || responseCode == 403) {
                // 401/403可能是因为认证问题，但服务是可达的
                log.info("HTTP连接可达但需要认证: {} (响应码: {})", url, responseCode);
                return true;
            } else {
                log.warn("HTTP连接测试失败: {} (响应码: {})", url, responseCode);
                return false;
            }
            
        } catch (IOException e) {
            log.error("HTTP连接测试失败: {}, 错误: {}", url, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("HTTP连接测试异常: {}", url, e);
            return false;
        }
    }

    /**
     * 测试Apollo配置中心连接
     * 
     * @param apolloServerUrl Apollo服务器地址
     * @param appId 应用ID
     * @return 连接测试结果
     */
    public static boolean testApolloConnection(String apolloServerUrl, String appId) {
        if (!StringUtils.hasText(apolloServerUrl)) {
            log.warn("Apollo服务器地址为空");
            return false;
        }

        try {
            // 构造Apollo健康检查URL
            String healthCheckUrl = apolloServerUrl;
            if (!healthCheckUrl.endsWith("/")) {
                healthCheckUrl += "/";
            }
            
            // Apollo配置中心常用的健康检查接口
            String[] testUrls = {
                healthCheckUrl + "health",                    // Spring Boot Actuator健康检查
                healthCheckUrl + "actuator/health",          // Spring Boot Actuator健康检查
                healthCheckUrl + "apollo/health",             // Apollo专用健康检查
                healthCheckUrl,                              // 直接访问根路径
                healthCheckUrl + "apps/" + (StringUtils.hasText(appId) ? appId : "SampleApp")  // 应用配置接口
            };

            for (String testUrl : testUrls) {
                log.info("尝试Apollo连接测试: {}", testUrl);
                if (testHttpConnection(testUrl)) {
                    log.info("Apollo连接测试成功: {} (通过URL: {})", apolloServerUrl, testUrl);
                    return true;
                }
            }
            
            // 如果所有标准URL都失败，尝试简单的HTTP连接测试
            log.info("尝试基础HTTP连接测试: {}", apolloServerUrl);
            if (testHttpConnection(apolloServerUrl)) {
                log.info("Apollo基础连接测试成功: {}", apolloServerUrl);
                return true;
            }
            
            log.warn("Apollo连接测试失败: 所有测试URL都无法访问 {}", apolloServerUrl);
            return false;
            
        } catch (Exception e) {
            log.error("Apollo连接测试异常: {}", apolloServerUrl, e);
            return false;
        }
    }

    /**
     * 测试HTTP连接（带重试机制）
     * 
     * @param url 目标URL
     * @param retryCount 重试次数
     * @return 连接测试结果
     */
    public static boolean testHttpConnectionWithRetry(String url, int retryCount) {
        for (int i = 0; i <= retryCount; i++) {
            if (i > 0) {
                log.info("HTTP连接测试重试 {}/{}: {}", i, retryCount, url);
            }
            
            boolean result = testHttpConnection(url);
            if (result) {
                return true;
            }
            
            if (i < retryCount) {
                try {
                    Thread.sleep(2000); // 等待2秒后重试
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("HTTP连接测试失败，已重试{}次: {}", retryCount, url);
        return false;
    }
}
