package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 采集结果实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@TableName("collect_result")
@Schema(name = "CollectResultEntity", description = "采集结果")
public class CollectResultEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    /**
     * 执行ID
     */
    @Schema(description = "执行ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "执行ID不能为空")
    private String executeId;

    /**
     * 服务器实例ID
     */
    @Schema(description = "服务器实例ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "服务器实例ID不能为空")
    private Long serverInstanceId;

    /**
     * 采集项名称
     */
    @Schema(description = "采集项名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "采集项名称不能为空")
    private String collectItemName;

    /**
     * 采集类型：COMMAND/FILE/API/APOLLO
     */
    @Schema(description = "采集类型", allowableValues = {"COMMAND", "FILE", "API", "APOLLO"})
    @NotBlank(message = "采集类型不能为空")
    private String collectType;

    /**
     * 采集内容
     */
    @Schema(description = "采集内容")
    private String collectContent;

    /**
     * 文件路径（文件采集时使用）
     */
    @Schema(description = "文件路径")
    private String filePath;

    /**
     * API端点（API采集时使用）
     */
    @Schema(description = "API端点")
    private String apiEndpoint;

    /**
     * 命名空间（Apollo采集时使用）
     */
    @Schema(description = "命名空间")
    private String namespace;

    /**
     * 采集状态：1成功 0失败
     */
    @Schema(description = "采集状态", allowableValues = {"0", "1"})
    @NotNull(message = "采集状态不能为空")
    private Integer collectStatus;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;

    /**
     * 执行时间
     */
    @Schema(description = "执行时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "执行时间不能为空")
    private LocalDateTime executeTime;

    /**
     * 执行耗时(毫秒)
     */
    @Schema(description = "执行耗时(毫秒)")
    private Long durationMs;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数", example = "0")
    private Integer retryCount;
}