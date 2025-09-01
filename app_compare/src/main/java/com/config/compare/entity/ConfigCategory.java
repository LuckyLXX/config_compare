package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 配置分类实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("config_category")
@Schema(name = "ConfigCategory", description = "配置分类")
public class ConfigCategory extends BaseEntity {

    /**
     * 分类名称
     */
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    /**
     * 分类编码
     */
    @Schema(description = "分类编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    /**
     * 父分类ID
     */
    @Schema(description = "父分类ID", example = "0")
    private Long parentId;

    /**
     * 适用服务器类型ID列表，逗号分隔
     */
    @Schema(description = "适用服务器类型ID列表")
    private String applicableTypes;

    /**
     * 分类描述
     */
    @Schema(description = "分类描述")
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    @Schema(description = "状态", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 排序号
     */
    @Schema(description = "排序号", example = "0")
    private Integer sortOrder;
}