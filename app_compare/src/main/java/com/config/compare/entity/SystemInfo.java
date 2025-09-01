package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 系统信息实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_system_info")
@Schema(name = "SystemInfo", description = "系统信息")
public class SystemInfo extends BaseEntity {

    /**
     * 系统名称
     */
    @Schema(description = "系统名称", required = true)
    @NotBlank(message = "系统名称不能为空")
    private String systemName;

    /**
     * 系统描述
     */
    @Schema(description = "系统描述")
    private String systemDesc;

    /**
     * 环境类型：UAT/PROD
     */
    @Schema(description = "环境类型", required = true, allowableValues = {"UAT", "PROD"})
    @NotBlank(message = "环境类型不能为空")
    private String envType;

    /**
     * 系统负责人
     */
    @Schema(description = "系统负责人")
    private String owner;

    /**
     * 联系方式
     */
    @Schema(description = "联系方式")
    private String contact;

    /**
     * 状态：1启用 0禁用
     */
    @Schema(description = "状态", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;
}