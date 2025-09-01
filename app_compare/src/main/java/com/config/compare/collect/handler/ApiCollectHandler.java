package com.config.compare.collect.handler;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

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

    @Autowired
    private RestTemplate restTemplate;

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
        
        if (!StringUtils.hasText(url)) {
            return false;
        }
        
        try {
            // 使用HEAD请求测试连接
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.HEAD, null, String.class);
            return response.getStatusCode().is2xxSuccessful();
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
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            if (params.containsKey("headers")) {
                Map<String, String> headerMap = (Map<String, String>) params.get("headers");
                headerMap.forEach(headers::add);
            }
            
            // 构建请求体
            String requestBody = (String) params.get("body");
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());
            ResponseEntity<String> response = restTemplate.exchange(
                url, httpMethod, requestEntity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                CollectResult result = CollectResult.success(response.getBody());
                result.setApiEndpoint(url);
                return result;
            } else {
                return CollectResult.fail("API调用失败，状态码：" + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("API调用失败", e);
            return CollectResult.fail("API调用失败：" + e.getMessage());
        }
    }
}