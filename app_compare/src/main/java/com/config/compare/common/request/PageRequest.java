package com.config.compare.common.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 分页查询请求
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@Schema(name = "PageRequest", description = "分页查询请求")
public class PageRequest {

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", required = true, example = "1")
    @NotNull(message = "当前页码不能为空")
    @Min(value = 1, message = "当前页码不能小于1")
    private Long current = 1L;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", required = true, example = "10")
    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小不能小于1")
    private Long size = 10L;

    /**
     * 搜索关键词
     */
    @Schema(description = "搜索关键词")
    private String keyword;

    /**
     * 排序字段
     */
    @Schema(description = "排序字段", example = "createTime")
    private String orderBy;

    /**
     * 排序方向
     */
    @Schema(description = "排序方向", allowableValues = {"ASC", "DESC"}, example = "DESC")
    private String orderDirection = "DESC";
}