package com.config.compare.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 数据处理响应DTO
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "DataProcessResponse", description = "数据处理响应")
public class DataProcessResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 处理类型：excel, ai, clean
     */
    @Schema(description = "处理类型：excel, ai, clean")
    private String type;

    /**
     * 处理是否成功
     */
    @Schema(description = "处理是否成功")
    private Boolean success;

    /**
     * 处理结果内容
     * - excel类型：文件下载ID
     * - ai类型：AI分析结果文本
     * - clean类型：清洗后的JSON数据
     */
    @Schema(description = "处理结果内容")
    private String content;

    /**
     * 文件ID（excel类型使用）
     */
    @Schema(description = "文件ID（用于下载）")
    private String fileId;

    /**
     * 文件名（excel类型使用）
     */
    @Schema(description = "文件名")
    private String fileName;

    /**
     * 原始数据大小（字节）
     */
    @Schema(description = "原始数据大小（字节）")
    private Long originalSize;

    /**
     * 处理后数据大小（字节）
     */
    @Schema(description = "处理后数据大小（字节）")
    private Long processedSize;

    /**
     * 处理耗时（毫秒）
     */
    @Schema(description = "处理耗时（毫秒）")
    private Long duration;

    /**
     * AI模型（ai类型使用）
     */
    @Schema(description = "使用的AI模型")
    private String model;

    /**
     * 应用的清洗规则（clean类型使用）
     */
    @Schema(description = "应用的清洗规则")
    private String appliedRules;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 创建成功响应
     */
    public static DataProcessResponse success(String type, String content) {
        return DataProcessResponse.builder()
                .type(type)
                .success(true)
                .content(content)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static DataProcessResponse error(String type, String errorMessage) {
        return DataProcessResponse.builder()
                .type(type)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
