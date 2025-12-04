package com.config.compare.collect.handler;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * HTTP API采集处理器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class ApiCollectHandler extends AbstractCollectHandler {

    @Override
    public String getTypeCode() {
        return "API";
    }

    @Override
    public String getTypeName() {
        return "HTTP接口调用";
    }

    @Override
    public boolean testConnection(CollectContext context) {
        Map<String, Object> params = context.getConfigParams();
        String url = (String) params.get("url");
        boolean ignoreSSL = Boolean.TRUE.equals(params.get("ignoreSSL"));
        
        if (!StringUtils.hasText(url)) {
            return false;
        }
        
        try {
            // 使用HEAD请求测试连接
            HttpRequest request = HttpUtil.createRequest(Method.HEAD, url);
            
            if (ignoreSSL) {
                request.setSSLSocketFactory(createIgnoreSSLSocketFactory());
            }
            
            // 设置超时
            request.timeout(5000);
            
            HttpResponse response = request.execute();
            return response.isOk();
        } catch (Exception e) {
            log.error("API连接测试失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    protected CollectResult doCollect(CollectContext context) {
        return executeWithRetry(context, () -> {
            Map<String, Object> params = context.getConfigParams();
            String url = (String) params.get("url");
            String method = (String) params.getOrDefault("method", "GET");
            
            if (!StringUtils.hasText(url)) {
                return CollectResult.fail("URL参数为空");
            }
            
            return callApi(context, url, method, params);
        });
    }

    @Override
    protected boolean validateConfigJson(JsonNode jsonNode) {
        return jsonNode.has("url") && StringUtils.hasText(jsonNode.get("url").asText());
    }

    @Override
    public String getConfigSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"url\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"API接口地址\"\n" +
               "    },\n" +
               "    \"method\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"HTTP方法\",\n" +
               "      \"enum\": [\"GET\", \"POST\", \"PUT\", \"DELETE\"],\n" +
               "      \"default\": \"GET\"\n" +
               "    },\n" +
               "    \"headers\": {\n" +
               "      \"type\": \"object\",\n" +
               "      \"description\": \"请求头\"\n" +
               "    },\n" +
               "    \"body\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"请求体（POST/PUT时使用）\"\n" +
               "    },\n" +
               "    \"timeout\": {\n" +
               "      \"type\": \"integer\",\n" +
               "      \"description\": \"超时时间（秒）\",\n" +
               "      \"default\": 30\n" +
               "    },\n" +
               "    \"ignoreSSL\": {\n" +
               "      \"type\": \"boolean\",\n" +
               "      \"description\": \"是否忽略SSL证书验证\",\n" +
               "      \"default\": false\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"url\"]\n" +
               "}";
    }

    @Override
    public String getDescription() {
        return "通过HTTP接口调用获取配置信息，支持GET、POST等方法";
    }

    /**
     * 调用API
     */
    @SuppressWarnings("unchecked")
    private CollectResult callApi(CollectContext context, String url, String method, Map<String, Object> params) {
        try {
            boolean ignoreSSL = Boolean.TRUE.equals(params.get("ignoreSSL"));
            int timeout = 30000; // 默认30秒
            if (params.get("timeout") != null) {
                timeout = Integer.parseInt(params.get("timeout").toString()) * 1000;
            }

            // 构建请求
            HttpRequest request = HttpUtil.createRequest(Method.valueOf(method.toUpperCase()), url);
            request.timeout(timeout);
            
            // 设置SSL忽略
            if (ignoreSSL) {
                request.setSSLSocketFactory(createIgnoreSSLSocketFactory());
                log.info("已启用SSL证书忽略模式，API: {}", url);
            }

            // 构建请求头，并获取Content-Type
            String contentType = null;
            if (params.containsKey("headers")) {
                Map<String, Object> headerMap = (Map<String, Object>) params.get("headers");
                for (Map.Entry<String, Object> entry : headerMap.entrySet()) {
                    if (entry.getValue() != null) {
                        request.header(entry.getKey(), entry.getValue().toString());
                        // 记录Content-Type
                        if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                            contentType = entry.getValue().toString().toLowerCase();
                        }
                    }
                }
            }
            
            // 构建请求体
            String requestBody = (String) params.get("body");
            if (StringUtils.hasText(requestBody)) {
                // 根据Content-Type决定发送方式
                if (contentType != null && (contentType.contains("multipart/form-data") 
                        || contentType.contains("application/x-www-form-urlencoded"))) {
                    // 表单格式：解析JSON为Map后使用form()发送
                    try {
                        Map<String, Object> formData = objectMapper.readValue(requestBody, Map.class);
                        for (Map.Entry<String, Object> entry : formData.entrySet()) {
                            if (entry.getValue() != null) {
                                request.form(entry.getKey(), entry.getValue().toString());
                            }
                        }
                        log.info("使用表单格式发送请求体，字段数: {}", formData.size());
                    } catch (Exception e) {
                        log.warn("解析请求体JSON失败，降级为原始body发送: {}", e.getMessage());
                        request.body(requestBody);
                    }
                } else {
                    // JSON或其他格式：直接发送原始body
                    request.body(requestBody);
                }
            }
            
            // 发送请求
            HttpResponse response = request.execute();
            
            if (response.isOk()) {
                String content = response.body();
                // 移除BOM字符
                if (content != null && content.length() > 0 && content.charAt(0) == '\uFEFF') {
                    content = content.substring(1);
                    log.info("检测到并移除了UTF-8 BOM字符，API: {}", url);
                }
                CollectResult result = CollectResult.success(content);
                result.setApiEndpoint(url);
                return result;
            } else {
                return CollectResult.fail("API调用失败，状态码：" + response.getStatus());
            }
            
        } catch (Exception e) {
            log.error("API调用失败", e);
            return CollectResult.fail("API调用失败：" + e.getMessage());
        }
    }

    /**
     * 创建一个信任所有证书的 SSLSocketFactory，仅在用户显式勾选忽略 SSL 时使用
     */
    private SSLSocketFactory createIgnoreSSLSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            log.error("创建忽略SSL证书的SSLSocketFactory失败", e);
            return null;
        }
    }
}