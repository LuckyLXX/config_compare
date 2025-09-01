package com.config.compare.compare.model;

import com.config.compare.entity.ConfigBaseline;
import com.config.compare.entity.ServerInstance;
import lombok.Data;

import java.util.Map;

/**
 * 比对上下文
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class CompareContext {

    /**
     * 基线配置
     */
    private ConfigBaseline baseline;

    /**
     * 基线内容
     */
    private String baselineContent;

    /**
     * 当前内容
     */
    private String currentContent;

    /**
     * 服务器实例
     */
    private ServerInstance serverInstance;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 比对规则
     */
    private Map<String, Object> compareRules;

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

    public CompareContext() {}

    public CompareContext(String baselineContent, String currentContent) {
        this.baselineContent = baselineContent;
        this.currentContent = currentContent;
    }

    public CompareContext(ConfigBaseline baseline, String currentContent, ServerInstance serverInstance) {
        this.baseline = baseline;
        this.baselineContent = baseline != null ? baseline.getConfigContent() : null;
        this.currentContent = currentContent;
        this.serverInstance = serverInstance;
    }
}