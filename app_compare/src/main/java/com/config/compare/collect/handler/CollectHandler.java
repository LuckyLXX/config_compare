package com.config.compare.collect.handler;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;

/**
 * 采集处理器接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CollectHandler {

    /**
     * 获取采集类型编码
     * 
     * @return 类型编码
     */
    String getTypeCode();

    /**
     * 获取采集类型名称
     * 
     * @return 类型名称
     */
    String getTypeName();

    /**
     * 测试连接
     * 
     * @param context 采集上下文
     * @return 连接测试结果
     */
    boolean testConnection(CollectContext context);

    /**
     * 执行采集
     * 
     * @param context 采集上下文
     * @return 采集结果
     */
    CollectResult collect(CollectContext context);

    /**
     * 验证配置参数
     * 
     * @param config 配置参数JSON
     * @return 验证是否通过
     */
    boolean validateConfig(String config);

    /**
     * 获取配置参数架构
     * 
     * @return JSON Schema字符串
     */
    String getConfigSchema();

    /**
     * 获取处理器描述
     * 
     * @return 描述信息
     */
    String getDescription();
}