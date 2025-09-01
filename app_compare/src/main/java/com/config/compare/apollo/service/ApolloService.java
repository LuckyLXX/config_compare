package com.config.compare.apollo.service;

import com.config.compare.apollo.model.ApolloConfig;
import com.config.compare.apollo.model.ApolloNamespace;
import java.util.List;

/**
 * Apollo服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ApolloService {

    /**
     * 测试Apollo连接
     * 
     * @param config Apollo配置
     * @return 是否连接成功
     */
    boolean testConnection(ApolloConfig config);

    /**
     * 获取应用的所有命名空间
     * 
     * @param config Apollo配置
     * @return 命名空间列表
     */
    List<String> getNamespaces(ApolloConfig config);

    /**
     * 获取指定命名空间的配置
     * 
     * @param config Apollo配置
     * @param namespace 命名空间
     * @return 命名空间配置
     */
    ApolloNamespace getNamespaceConfigs(ApolloConfig config, String namespace);

    /**
     * 获取指定命名空间的发布配置
     * 
     * @param config Apollo配置
     * @param namespace 命名空间
     * @return 发布的配置内容
     */
    String getPublishedConfigs(ApolloConfig config, String namespace);

    /**
     * 预览Apollo采集结果
     * 
     * @param config Apollo配置
     * @return 预览结果
     */
    List<ApolloNamespace> previewConfigs(ApolloConfig config);
}