package com.config.compare.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 数据清洗请求DTO
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
@Data
@Schema(name = "DataCleanRequest", description = "数据清洗请求")
public class DataCleanRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源数据（JSON格式字符串）
     */
    @NotBlank(message = "源数据不能为空")
    @Schema(description = "源数据（JSON格式字符串）", required = true)
    private String sourceData;

    /**
     * 清洗规则列表
     * 可选值：remove_null, trim_string, remove_duplicates, format_date
     */
    @NotEmpty(message = "请至少选择一个清洗规则")
    @Schema(description = "清洗规则列表", required = true)
    private List<String> rules;

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
     * 日期格式（format_date规则使用）
     */
    @Schema(description = "目标日期格式", defaultValue = "yyyy-MM-dd HH:mm:ss")
    private String dateFormat = "yyyy-MM-dd HH:mm:ss";
}
