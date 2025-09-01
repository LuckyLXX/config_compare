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
 * 比对任务实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compare_task")
@Schema(name = "CompareTask", description = "比对任务")
public class CompareTask extends BaseEntity {

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
     * 配置分类ID
     */
    @Schema(description = "配置分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "配置分类ID不能为空")
    private Long categoryId;

    /**
     * 指定基线ID（为空则使用默认基线）
     */
    @Schema(description = "指定基线ID")
    private Long baselineId;

    /**
     * 关联采集任务ID
     */
    @Schema(description = "关联采集任务ID")
    private Long collectTaskId;

    /**
     * 比对规则JSON
     */
    @Schema(description = "比对规则JSON")
    private String compareRules;

    /**
     * 执行类型：1立即执行 2定时执行 3触发执行
     */
    @Schema(description = "执行类型", allowableValues = {"1", "2", "3"})
    @NotNull(message = "执行类型不能为空")
    private Integer executeType;

    /**
     * Cron表达式
     */
    @Schema(description = "Cron表达式")
    private String cronExpression;

    /**
     * 是否自动执行：1是 0否
     */
    @Schema(description = "是否自动执行", allowableValues = {"0", "1"})
    private Integer autoExecute;

    /**
     * 任务状态：1启用 0禁用
     */
    @Schema(description = "任务状态", allowableValues = {"0", "1"})
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