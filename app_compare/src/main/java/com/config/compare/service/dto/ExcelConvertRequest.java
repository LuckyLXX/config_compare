package com.config.compare.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * JSON转Excel请求DTO
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
@Data
@Schema(name = "ExcelConvertRequest", description = "JSON转Excel请求")
public class ExcelConvertRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源数据（JSON格式字符串）
     */
    @NotBlank(message = "源数据不能为空")
    @Schema(description = "源数据（JSON格式字符串）", required = true)
    private String sourceData;

    /**
     * 转换模式：auto-自动识别表头，custom-自定义映射
     */
    @Schema(description = "转换模式：auto-自动识别表头，custom-自定义映射", defaultValue = "auto")
    private String mode = "auto";

    /**
     * 自定义字段映射（JSON Path -> Excel Column）
     * 格式：{"users[].name": "姓名", "users[].age": "年龄"}
     */
    @Schema(description = "自定义字段映射（mode=custom时生效）")
    private String mapping;

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
     * 文件名（可选）
     */
    @Schema(description = "导出文件名", defaultValue = "data")
    private String fileName = "data";
}
