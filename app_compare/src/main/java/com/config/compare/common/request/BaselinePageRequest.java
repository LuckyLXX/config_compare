package com.config.compare.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基线分页查询请求
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "BaselinePageRequest", description = "基线分页查询请求")
public class BaselinePageRequest extends PageRequest {

    /**
     * 系统ID
     */
    @Schema(description = "系统ID")
    private Long systemId;

    /**
     * 服务器类型ID
     */
    @Schema(description = "服务器类型ID")
    private Long serverTypeId;

    /**
     * 配置分类ID
     */
    @Schema(description = "配置分类ID")
    private Long categoryId;

    /**
     * 基线名称（模糊查询）
     */
    @Schema(description = "基线名称")
    private String baselineName;

    /**
     * 状态（0-草稿，1-生效，2-归档）
     */
    @Schema(description = "状态")
    private Integer status;

    /**
     * 是否默认（0-否，1-是）
     */
    @Schema(description = "是否默认")
    private Integer isDefault;
}

