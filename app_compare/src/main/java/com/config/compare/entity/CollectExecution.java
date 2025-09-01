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
 * 采集执行记录实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@TableName("collect_execution")
@Schema(name = "CollectExecution", description = "采集执行记录")
public class CollectExecution implements Serializable {

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
     * 执行状态：1成功 2部分成功 3失败 4运行中
     */
    @Schema(description = "执行状态", allowableValues = {"1", "2", "3", "4"})
    @NotNull(message = "执行状态不能为空")
    private Integer executeStatus;

    /**
     * 总服务器数
     */
    @Schema(description = "总服务器数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "总服务器数不能为空")
    private Integer totalServers;

    /**
     * 成功服务器数
     */
    @Schema(description = "成功服务器数", example = "0")
    private Integer successServers;

    /**
     * 失败服务器数
     */
    @Schema(description = "失败服务器数", example = "0")
    private Integer failedServers;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    /**
     * 执行耗时(毫秒)
     */
    @Schema(description = "执行耗时(毫秒)")
    private Long durationMs;

    /**
     * 错误信息
     */
    @Schema(description = "错误信息")
    private String errorMessage;
}