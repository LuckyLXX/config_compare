package com.config.compare.apollo.model;

import lombok.Data;
import java.util.List;

/**
 * Apollo命名空间
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class ApolloNamespace {

    /**
     * 应用标识
     */
    private String appId;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 命名空间名称
     */
    private String namespaceName;

    /**
     * 命名空间格式（properties/xml/yml/yaml/json/txt）
     */
    private String format;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 配置项列表
     */
    private List<ApolloConfigItem> items;

    /**
     * 发布信息
     */
    private String releaseKey;

    /**
     * 发布标题
     */
    private String releaseTitle;

    /**
     * 发布注释
     */
    private String releaseComment;

    public ApolloNamespace() {}

    public ApolloNamespace(String appId, String clusterName, String namespaceName) {
        this.appId = appId;
        this.clusterName = clusterName;
        this.namespaceName = namespaceName;
    }
}