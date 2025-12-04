package com.config.compare.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 配置分类分页查询请求
 * 扩展PageRequest，增加分类特有的筛选条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "CategoryPageRequest", description = "配置分类分页查询请求")
public class CategoryPageRequest extends PageRequest {

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "分类编码")
    private String categoryCode;

    @Schema(description = "状态：1启用 0禁用")
    private Integer status;
}

