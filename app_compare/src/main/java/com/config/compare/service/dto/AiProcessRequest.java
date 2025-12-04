package com.config.compare.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * AI智能处理请求DTO
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
@Data
@Schema(name = "AiProcessRequest", description = "AI智能处理请求")
public class AiProcessRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源数据（JSON格式字符串）
     */
    @NotBlank(message = "源数据不能为空")
    @Schema(description = "源数据（JSON格式字符串）", required = true)
    private String sourceData;

    /**
     * AI模型：deepseek, gpt4, claude3
     */
    @NotBlank(message = "请选择AI模型")
    @Schema(description = "AI模型：deepseek, gpt4, claude3", required = true)
    private String model;

    /**
     * 处理指令（Prompt）
     */
    @NotBlank(message = "请输入处理指令")
    @Schema(description = "处理指令（Prompt）", required = true)
    private String prompt;

    /**
     * 任务ID（可选，用于关联采集任务）
     */
    @Schema(description = "任务ID")
    private Long taskId;

    /**
     * 执行ID（可选，用于关联采集执行）
     */
    @Schema(description = "执行ID")
    private String executeId;

    /**
     * 最大Token数（可选）
     */
    @Schema(description = "最大Token数", defaultValue = "4096")
    private Integer maxTokens = 4096;

    /**
     * 温度参数（可选，控制创造性，0-1之间）
     */
    @Schema(description = "温度参数", defaultValue = "0.7")
    private Double temperature = 0.7;

    /**
     * 自定义API地址（可选，前端配置的模型URL）
     */
    @Schema(description = "自定义API地址")
    private String customUrl;

    /**
     * 自定义API Key（可选，前端配置的API Key）
     */
    @Schema(description = "自定义API Key")
    private String customApiKey;

    /**
     * 自定义模型标识（可选，API请求使用的model参数，如moonshot-v1-8k）
     */
    @Schema(description = "自定义模型标识")
    private String customModelId;

    /**
     * 请求超时时间（秒），默认60秒
     */
    @Schema(description = "请求超时时间（秒）", defaultValue = "60")
    private Integer timeout = 60;
}
