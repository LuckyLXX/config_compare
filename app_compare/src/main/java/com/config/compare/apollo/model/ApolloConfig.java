package com.config.compare.apollo.model;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Apollo配置信息
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class ApolloConfig {

    /**
     * Apollo服务器地址
     */
    @NotBlank(message = "Apollo服务器地址不能为空")
    private String serverUrl;

    /**
     * 应用标识
     */
    @NotBlank(message = "应用标识不能为空")
    private String appId;

    /**
     * 集群名称
     */
    private String cluster = "default";

    /**
     * 环境
     */
    @NotBlank(message = "环境不能为空")
    private String env;

    /**
     * 命名空间列表
     */
    @NotNull(message = "命名空间列表不能为空")
    private List<String> namespaces;

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 10000;

    /**
     * 是否启用SSL验证
     */
    private Boolean enableSsl = true;

    public ApolloConfig() {}

    public ApolloConfig(String serverUrl, String appId, String env, List<String> namespaces) {
        this.serverUrl = serverUrl;
        this.appId = appId;
        this.env = env;
        this.namespaces = namespaces;
    }

    public ApolloConfig(String serverUrl, String appId, String cluster, String env, List<String> namespaces, String token) {
        this.serverUrl = serverUrl;
        this.appId = appId;
        this.cluster = cluster;
        this.env = env;
        this.namespaces = namespaces;
        this.token = token;
    }
}