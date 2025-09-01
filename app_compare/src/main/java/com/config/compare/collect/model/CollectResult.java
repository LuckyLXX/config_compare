package com.config.compare.collect.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采集结果
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class CollectResult {

    /**
     * 采集是否成功
     */
    private boolean success;

    /**
     * 采集内容
     */
    private String content;

    /**
     * 文件路径（文件采集时使用）
     */
    private String filePath;

    /**
     * API端点（API采集时使用）
     */
    private String apiEndpoint;

    /**
     * 命名空间（Apollo采集时使用）
     */
    private String namespace;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 重试次数
     */
    private int retryCount;

    /**
     * 扩展信息
     */
    private String extendInfo;

    public CollectResult() {
        this.executeTime = LocalDateTime.now();
    }

    public CollectResult(boolean success) {
        this();
        this.success = success;
    }

    public CollectResult(boolean success, String content) {
        this(success);
        this.content = content;
    }

    public CollectResult(boolean success, String content, String errorMessage) {
        this(success, content);
        this.errorMessage = errorMessage;
    }

    /**
     * 创建成功的采集结果
     */
    public static CollectResult success(String content) {
        return new CollectResult(true, content);
    }

    /**
     * 创建失败的采集结果
     */
    public static CollectResult fail(String errorMessage) {
        return new CollectResult(false, null, errorMessage);
    }

    /**
     * 创建错误的采集结果
     */
    public static CollectResult error(String errorMessage) {
        return new CollectResult(false, null, errorMessage);
    }

    /**
     * 设置文件路径（支持链式调用）
     */
    public CollectResult setFilePath(String filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * 设置API端点（支持链式调用）
     */
    public CollectResult setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
        return this;
    }

    /**
     * 设置命名空间（支持链式调用）
     */
    public CollectResult setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * 设置执行耗时
     */
    public void setDuration(long startTimeMs) {
        this.durationMs = System.currentTimeMillis() - startTimeMs;
    }
}