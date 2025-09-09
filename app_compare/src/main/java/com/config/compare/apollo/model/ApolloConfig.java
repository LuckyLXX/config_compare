package com.config.compare.apollo.model;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Apollo配置信息 - 简化版
 * 
 * @author system
 * @version 2.0.0
 * @since 2025-01-25
 */
@Data
public class ApolloConfig {

    /**
     * Apollo Config Service地址
     */
    @NotBlank(message = "Apollo Config Service地址不能为空")
    private String configServiceUrl;

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
     * 命名空间列表
     */
    private List<String> namespaces;

    /**
     * 访问密钥（用于签名认证）
     */
    private String secret;

    /**
     * 连接超时时间（毫秒）
     */
    private Integer connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private Integer readTimeout = 10000;

    public ApolloConfig() {}

    public ApolloConfig(String configServiceUrl, String appId, String cluster, List<String> namespaces) {
        this.configServiceUrl = configServiceUrl;
        this.appId = appId;
        this.cluster = cluster;
        this.namespaces = namespaces;
    }
}