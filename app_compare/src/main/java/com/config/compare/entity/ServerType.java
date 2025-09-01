package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 服务器类型实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("server_type")
@Schema(name = "ServerType", description = "服务器类型")
public class ServerType extends BaseEntity {

    /**
     * 服务器类型名称
     */
    @Schema(description = "服务器类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务器类型名称不能为空")
    private String typeName;

    /**
     * 服务器类型编码
     */
    @Schema(description = "服务器类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "服务器类型编码不能为空")
    private String typeCode;

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