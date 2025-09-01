package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 采集任务实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collect_task")
@Schema(name = "CollectTask", description = "采集任务")
public class CollectTask extends BaseEntity {

    /**
     * 任务名称
     */
    @Schema(description = "任务名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    /**
     * 系统ID
     */
    @Schema(description = "系统ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "系统ID不能为空")
    private Long systemId;

    /**
     * 服务器类型ID列表，逗号分隔
     */
    @Schema(description = "服务器类型ID列表")
    private String serverTypeIds;

    /**
     * 服务器实例ID列表，逗号分隔
     */
    @Schema(description = "服务器实例ID列表")
    private String serverInstanceIds;

    /**
     * 模板ID
     */
    @Schema(description = "模板ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    /**
     * Cron表达式
     */
    @Schema(description = "Cron表达式")
    private String cronExpression;

    /**
     * 执行类型：1立即执行 2定时执行
     */
    @Schema(description = "执行类型", allowableValues = {"1", "2"})
    @NotNull(message = "执行类型不能为空")
    private Integer executeType;

    /**
     * 最大并发数
     */
    @Schema(description = "最大并发数", example = "5")
    private Integer maxConcurrency;

    /**
     * 超时时间（秒）
     */
    @Schema(description = "超时时间（秒）", example = "300")
    private Integer timeoutSeconds;

    /**
     * 重试次数
     */
    @Schema(description = "重试次数", example = "2")
    private Integer retryCount;

    /**
     * 任务状态：1启用 0禁用
     */
    @Schema(description = "任务状态", allowableValues = {"0", "1"})
    @NotNull(message = "任务状态不能为空")
    private Integer status;

    /**
     * 最后执行时间
     */
    @Schema(description = "最后执行时间")
    private LocalDateTime lastExecuteTime;

    /**
     * 下次执行时间
     */
    @Schema(description = "下次执行时间")
    private LocalDateTime nextExecuteTime;

    /**
     * 任务描述
     */
    @Schema(description = "任务描述")
    private String description;
}