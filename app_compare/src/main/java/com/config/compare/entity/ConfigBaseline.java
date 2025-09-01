package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 配置基线实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("config_baseline")
@Schema(name = "ConfigBaseline", description = "配置基线")
public class ConfigBaseline extends BaseEntity {

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
     * 基线名称
     */
    @Schema(description = "基线名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "基线名称不能为空")
    private String baselineName;

    /**
     * 基线版本
     */
    @Schema(description = "基线版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "基线版本不能为空")
    private String baselineVersion;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String fileName;

    /**
     * 配置内容
     */
    @Schema(description = "配置内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置内容不能为空")
    private String configContent;

    /**
     * 配置内容哈希值
     */
    @Schema(description = "配置内容哈希值")
    private String configHash;

    /**
     * 是否默认版本：1是 0否
     */
    @Schema(description = "是否默认版本", allowableValues = {"0", "1"})
    private Integer isDefault;

    /**
     * 状态：0草稿 1生效 2归档
     */
    @Schema(description = "状态", allowableValues = {"0", "1", "2"})
    private Integer status;

    /**
     * 基线描述
     */
    @Schema(description = "基线描述")
    private String description;

    /**
     * 来源类型：MANUAL/IMPORT/COPY
     */
    @Schema(description = "来源类型", allowableValues = {"MANUAL", "IMPORT", "COPY"})
    private String sourceType;

    /**
     * 来源基线ID（复制时使用）
     */
    @Schema(description = "来源基线ID")
    private Long sourceBaselineId;
}