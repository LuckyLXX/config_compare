package com.config.compare.apollo.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpException;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.model.ApolloConfigItem;
import com.config.compare.apollo.model.ApolloNamespace;
import com.config.compare.apollo.service.ApolloService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Apollo服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class ApolloServiceImpl implements ApolloService {

    @Override
    public boolean testConnection(ApolloConfig config) {
        try {
            // 尝试获取应用信息来测试连接
            String url = buildUrl(config, "/openapi/v1/apps/" + config.getAppId());
            String response = doGetRequest(config, url);
            return StrUtil.isNotBlank(response);
        } catch (Exception e) {
            log.warn("Apollo连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> getNamespaces(ApolloConfig config) {
        try {
            String url = buildUrl(config, String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces",
                    config.getEnv(), config.getAppId(), config.getCluster()));
            String response = doGetRequest(config, url);
            
            List<String> namespaces = new ArrayList<>();
            JSONArray jsonArray = JSONUtil.parseArray(response);
            for (Object item : jsonArray) {
                JSONObject namespace = (JSONObject) item;
                namespaces.add(namespace.getStr("namespaceName"));
            }
            
            return namespaces;
        } catch (Exception e) {
            log.error("获取Apollo命名空间列表失败: {}", e.getMessage());
            throw new RuntimeException("获取Apollo命名空间列表失败: " + e.getMessage());
        }
    }

    @Override
    public ApolloNamespace getNamespaceConfigs(ApolloConfig config, String namespace) {
        try {
            // 获取命名空间配置项
            String itemsUrl = buildUrl(config, String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/items",
                    config.getEnv(), config.getAppId(), config.getCluster(), namespace));
            String itemsResponse = doGetRequest(config, itemsUrl);

            ApolloNamespace apolloNamespace = new ApolloNamespace();
            apolloNamespace.setAppId(config.getAppId());
            apolloNamespace.setClusterName(config.getCluster());
            apolloNamespace.setNamespaceName(namespace);

            // 解析配置项
            List<ApolloConfigItem> items = new ArrayList<>();
            JSONArray jsonArray = JSONUtil.parseArray(itemsResponse);
            for (Object item : jsonArray) {
                JSONObject configItem = (JSONObject) item;
                
                ApolloConfigItem apolloItem = new ApolloConfigItem();
                apolloItem.setKey(configItem.getStr("key"));
                apolloItem.setValue(configItem.getStr("value"));
                apolloItem.setComment(configItem.getStr("comment"));
                apolloItem.setDataChangeLastModifiedTime(configItem.getStr("dataChangeLastModifiedTime"));
                apolloItem.setDataChangeLastModifiedBy(configItem.getStr("dataChangeLastModifiedBy"));
                apolloItem.setDataChangeCreatedTime(configItem.getStr("dataChangeCreatedTime"));
                apolloItem.setDataChangeCreatedBy(configItem.getStr("dataChangeCreatedBy"));
                
                items.add(apolloItem);
            }
            apolloNamespace.setItems(items);

            // 尝试获取发布信息
            try {
                String releaseUrl = buildUrl(config, String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/releases/latest",
                        config.getEnv(), config.getAppId(), config.getCluster(), namespace));
                String releaseResponse = doGetRequest(config, releaseUrl);
                
                JSONObject releaseInfo = JSONUtil.parseObj(releaseResponse);
                apolloNamespace.setReleaseKey(releaseInfo.getStr("releaseKey"));
                apolloNamespace.setReleaseTitle(releaseInfo.getStr("name"));
                apolloNamespace.setReleaseComment(releaseInfo.getStr("comment"));
            } catch (Exception e) {
                log.warn("获取命名空间 {} 发布信息失败: {}", namespace, e.getMessage());
            }

            return apolloNamespace;
        } catch (Exception e) {
            log.error("获取Apollo命名空间配置失败: {}", e.getMessage());
            throw new RuntimeException("获取Apollo命名空间配置失败: " + e.getMessage());
        }
    }

    @Override
    public String getPublishedConfigs(ApolloConfig config, String namespace) {
        try {
            String url = buildUrl(config, String.format("/openapi/v1/envs/%s/apps/%s/clusters/%s/namespaces/%s/releases/latest",
                    config.getEnv(), config.getAppId(), config.getCluster(), namespace));
            String response = doGetRequest(config, url);
            
            JSONObject releaseInfo = JSONUtil.parseObj(response);
            return releaseInfo.getStr("configurations");
        } catch (Exception e) {
            log.error("获取Apollo发布配置失败: {}", e.getMessage());
            throw new RuntimeException("获取Apollo发布配置失败: " + e.getMessage());
        }
    }

    @Override
    public List<ApolloNamespace> previewConfigs(ApolloConfig config) {
        List<ApolloNamespace> result = new ArrayList<>();
        
        for (String namespace : config.getNamespaces()) {
            try {
                ApolloNamespace namespaceData = getNamespaceConfigs(config, namespace);
                result.add(namespaceData);
            } catch (Exception e) {
                log.error("预览命名空间 {} 配置失败: {}", namespace, e.getMessage());
                // 创建一个错误的命名空间对象
                ApolloNamespace errorNamespace = new ApolloNamespace();
                errorNamespace.setAppId(config.getAppId());
                errorNamespace.setClusterName(config.getCluster());
                errorNamespace.setNamespaceName(namespace);
                
                // 添加错误信息作为配置项
                ApolloConfigItem errorItem = new ApolloConfigItem();
                errorItem.setKey("ERROR");
                errorItem.setValue(e.getMessage());
                errorNamespace.setItems(List.of(errorItem));
                
                result.add(errorNamespace);
            }
        }
        
        return result;
    }

    /**
     * 构建请求URL
     */
    private String buildUrl(ApolloConfig config, String path) {
        String baseUrl = config.getServerUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    /**
     * 执行GET请求
     */
    private String doGetRequest(ApolloConfig config, String url) throws IOException {
        OkHttpClient client = buildHttpClient(config);
        
        Request.Builder requestBuilder = new Request.Builder().url(url);
        
        // 添加认证头
        if (StrUtil.isNotBlank(config.getToken())) {
            requestBuilder.addHeader("Authorization", config.getToken());
        }
        
        Request request = requestBuilder.build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new HttpException("Apollo请求失败: " + response.code() + " " + response.message());
            }
            
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new HttpException("Apollo响应体为空");
            }
            
            return responseBody.string();
        }
    }

    /**
     * 构建HTTP客户端
     */
    private OkHttpClient buildHttpClient(ApolloConfig config) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout() != null ? config.getConnectTimeout() : 5000, TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout() != null ? config.getReadTimeout() : 10000, TimeUnit.MILLISECONDS)
                .writeTimeout(10000, TimeUnit.MILLISECONDS);

        // SSL配置
        if (config.getEnableSsl() != null && !config.getEnableSsl()) {
            // 如果禁用SSL验证，这里可以添加相应的配置
            // 注意：生产环境不建议禁用SSL验证
        }

        return builder.build();
    }
}