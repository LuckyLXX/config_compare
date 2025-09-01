package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.config.compare.entity.base.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 服务器实例实体类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("server_instance")
@Schema(name = "ServerInstance", description = "服务器实例")
public class ServerInstance extends BaseEntity {

    /**
     * 系统ID
     */
    @Schema(description = "系统ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "系统ID不能为空")
    private Long systemId;

    /**
     * 服务器类型ID
     */
    @Schema(description = "服务器类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "服务器类型ID不能为空")
    private Long serverTypeId;

    /**
     * 实例名称
     */
    @Schema(description = "实例名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "实例名称不能为空")
    private String instanceName;

    /**
     * 服务器IP（SSH/SFTP类型使用）
     */
    @Schema(description = "服务器IP")
    private String serverIp;

    /**
     * SSH端口
     */
    @Schema(description = "SSH端口", example = "22")
    private Integer sshPort;

    /**
     * 连接用户名
     */
    @Schema(description = "连接用户名")
    private String username;

    /**
     * 连接密码（加密存储）
     */
    @Schema(description = "连接密码")
    private String password;

    /**
     * 服务器角色：MASTER/SLAVE/BACKUP
     */
    @Schema(description = "服务器角色", allowableValues = {"MASTER", "SLAVE", "BACKUP"})
    private String serverRole;

    /**
     * Apollo服务器地址
     */
    @Schema(description = "Apollo服务器地址")
    private String apolloServerUrl;

    /**
     * Apollo应用标识
     */
    @Schema(description = "Apollo应用标识")
    private String apolloAppId;

    /**
     * Apollo集群名称
     */
    @Schema(description = "Apollo集群名称")
    private String apolloCluster;

    /**
     * Apollo环境
     */
    @Schema(description = "Apollo环境")
    private String apolloEnv;

    /**
     * Apollo命名空间列表，逗号分隔
     */
    @Schema(description = "Apollo命名空间列表")
    private String apolloNamespaces;

    /**
     * Apollo访问令牌
     */
    @Schema(description = "Apollo访问令牌")
    private String apolloToken;

    /**
     * 自定义配置参数JSON
     */
    @Schema(description = "自定义配置参数JSON")
    private String customConfig;

    /**
     * 状态：1启用 0异常
     */
    @Schema(description = "状态", allowableValues = {"0", "1"})
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 最后连接时间
     */
    @Schema(description = "最后连接时间")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastConnectTime;

    /**
     * 连接状态：1正常 0异常
     */
    @Schema(description = "连接状态", allowableValues = {"0", "1"})
    private Integer connectStatus;

    /**
     * 实例描述
     */
    @Schema(description = "实例描述")
    private String description;
}