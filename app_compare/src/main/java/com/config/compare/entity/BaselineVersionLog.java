package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 基线版本切换日志实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("baseline_version_log")
@Schema(name = "BaselineVersionLog", description = "基线版本切换日志")
public class BaselineVersionLog extends BaseEntity {

    /**
     * 系统ID
     */
    @Schema(description = "系统ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "系统ID不能为空")
    private Long systemId;

    /**
     * 服务器类型ID
     */
    @Schema(description = "服务器类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "服务器类型ID不能为空")
    private Long serverTypeId;

    /**
     * 配置分类ID
     */
    @Schema(description = "配置分类ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "配置分类ID不能为空")
    private Long categoryId;

    /**
     * 原默认基线ID
     */
    @Schema(description = "原默认基线ID")
    private Long oldBaselineId;

    /**
     * 新默认基线ID
     */
    @Schema(description = "新默认基线ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新默认基线ID不能为空")
    private Long newBaselineId;

    /**
     * 原版本号
     */
    @Schema(description = "原版本号")
    private String oldVersion;

    /**
     * 新版本号
     */
    @Schema(description = "新版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "新版本号不能为空")
    private String newVersion;

    /**
     * 切换原因
     */
    @Schema(description = "切换原因")
    private String switchReason;

    /**
     * 操作类型：SWITCH/ROLLBACK
     */
    @Schema(description = "操作类型", allowableValues = {"SWITCH", "ROLLBACK"})
    @NotBlank(message = "操作类型不能为空")
    private String operationType;
}