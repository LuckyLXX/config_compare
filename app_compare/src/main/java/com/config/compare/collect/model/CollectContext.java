package com.config.compare.collect.model;

import com.config.compare.entity.ServerInstance;
import lombok.Data;

import java.util.Map;

/**
 * 采集上下文
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class CollectContext {

    /**
     * 服务器实例
     */
    private ServerInstance serverInstance;

    /**
     * 采集项名称
     */
    private String collectItemName;

    /**
     * 采集类型
     */
    private String collectType;

    /**
     * 采集配置参数
     */
    private Map<String, Object> configParams;

    /**
     * 超时时间（秒）
     */
    private int timeoutSeconds = 300;

    /**
     * 重试次数
     */
    private int retryCount = 2;

    /**
     * 执行ID
     */
    private String executeId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 扩展参数
     */
    private Map<String, Object> extendParams;

    public CollectContext() {}

    public CollectContext(ServerInstance serverInstance, String collectItemName, String collectType) {
        this.serverInstance = serverInstance;
        this.collectItemName = collectItemName;
        this.collectType = collectType;
    }
}