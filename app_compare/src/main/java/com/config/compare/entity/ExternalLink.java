package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 外部链接实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_external_link")
@Schema(name = "ExternalLink", description = "外部链接")
public class ExternalLink extends BaseEntity {

    /**
     * 链接名称/标题
     */
    @Schema(description = "链接名称", required = true)
    @NotBlank(message = "链接名称不能为空")
    private String linkName;

    /**
     * 链接URL地址
     */
    @Schema(description = "链接URL地址", required = true)
    @NotBlank(message = "链接地址不能为空")
    private String linkUrl;

    /**
     * 图标名称(Element Plus图标)
     */
    @Schema(description = "图标名称")
    private String icon;

    /**
     * 打开方式：1-内嵌iframe 2-新窗口打开
     */
    @Schema(description = "打开方式", allowableValues = {"1", "2"})
    @NotNull(message = "打开方式不能为空")
    private Integer openType;

    /**
     * 排序序号，数字越小越靠前
     */
    @Schema(description = "排序序号")
    private Integer sortOrder;

    /**
     * 父级ID，0表示一级菜单
     */
    @Schema(description = "父级ID")
    private Long parentId;

    /**
     * 链接描述
     */
    @Schema(description = "链接描述")
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    @Schema(description = "状态", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;
}
