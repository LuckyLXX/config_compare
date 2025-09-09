package com.config.compare.apollo.service;

import com.config.compare.apollo.model.ApolloConfig;
import java.util.Map;

/**
 * Apollo服务接口 - 简化版
 * 
 * @author system
 * @version 2.0.0
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
     * 获取指定命名空间的配置
     * 
     * @param config Apollo配置
     * @param namespace 命名空间
     * @return 配置键值对
     */
    Map<String, String> getNamespaceConfigs(ApolloConfig config, String namespace);

    /**
     * 获取所有命名空间的配置
     * 
     * @param config Apollo配置
     * @return 所有配置
     */
    Map<String, Map<String, String>> getAllConfigs(ApolloConfig config);
}