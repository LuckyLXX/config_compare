package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 采集模板实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collect_template")
@Schema(name = "CollectTemplate", description = "采集模板")
public class CollectTemplate extends BaseEntity {

    /**
     * 模板名称
     */
    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    /**
     * 模板类型：SERVER_CONFIG/FILE_CONFIG/API_CONFIG/APOLLO/MULTI_TYPE
     */
    @Schema(description = "模板类型", allowableValues = {"SERVER_CONFIG", "FILE_CONFIG", "API_CONFIG", "APOLLO", "MULTI_TYPE"})
    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    /**
     * 模板内容JSON
     */
    @Schema(description = "模板内容JSON", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板内容不能为空")
    private String templateContent;

    /**
     * 适用服务器类型列表
     */
    @Schema(description = "适用服务器类型列表")
    private String applicableServerTypes;

    /**
     * 扩展配置参数JSON
     */
    @Schema(description = "扩展配置参数JSON")
    private String configParams;

    /**
     * 模板描述
     */
    @Schema(description = "模板描述")
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    @Schema(description = "状态", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;
}