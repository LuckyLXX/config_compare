package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 差异详情实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("diff_detail")
@Schema(name = "DiffDetail", description = "差异详情")
public class DiffDetail extends BaseEntity {

    /**
     * 比对结果ID
     */
    @Schema(description = "比对结果ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "比对结果ID不能为空")
    private Long resultId;

    /**
     * 差异类型：ADD/DELETE/MODIFY
     */
    @Schema(description = "差异类型", allowableValues = {"ADD", "DELETE", "MODIFY"})
    @NotBlank(message = "差异类型不能为空")
    private String diffType;

    /**
     * 差异路径
     */
    @Schema(description = "差异路径")
    private String diffPath;

    /**
     * 差异键名
     */
    @Schema(description = "差异键名")
    private String diffKey;

    /**
     * 基线值
     */
    @Schema(description = "基线值")
    private String baselineValue;

    /**
     * 当前值
     */
    @Schema(description = "当前值")
    private String currentValue;

    /**
     * 差异级别：HIGH/MEDIUM/LOW
     */
    @Schema(description = "差异级别", allowableValues = {"HIGH", "MEDIUM", "LOW"})
    @NotBlank(message = "差异级别不能为空")
    private String diffLevel;

    /**
     * 差异分类
     */
    @Schema(description = "差异分类")
    private String diffCategory;

    /**
     * 建议操作
     */
    @Schema(description = "建议操作")
    private String suggestAction;
}