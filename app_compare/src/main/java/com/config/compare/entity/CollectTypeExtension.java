package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 采集类型扩展实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collect_type_extension")
@Schema(name = "CollectTypeExtension", description = "采集类型扩展")
public class CollectTypeExtension extends BaseEntity {

    /**
     * 类型编码
     */
    @Schema(description = "类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型编码不能为空")
    private String typeCode;

    /**
     * 类型名称
     */
    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    /**
     * 类型分类：BASIC/EXTENDED/CUSTOM
     */
    @Schema(description = "类型分类", allowableValues = {"BASIC", "EXTENDED", "CUSTOM"})
    @NotBlank(message = "类型分类不能为空")
    private String typeCategory;

    /**
     * 处理器类名
     */
    @Schema(description = "处理器类名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "处理器类名不能为空")
    private String handlerClass;

    /**
     * 配置参数架构JSON Schema
     */
    @Schema(description = "配置参数架构JSON Schema")
    private String configSchema;

    /**
     * 类型描述
     */
    @Schema(description = "类型描述")
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    @Schema(description = "状态", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;
}