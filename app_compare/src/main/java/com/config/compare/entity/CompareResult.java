package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比对结果实体类
 */
@Data
@TableName("compare_result")
public class CompareResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 比对任务ID
     */
    private Long taskId;

    /**
     * 比对执行ID
     */
    private Long executeId;

    /**
     * 目标服务器ID
     */
    private Long serverId;

    /**
     * 基线配置内容
     */
    private String baselineContent;

    /**
     * 采集配置内容
     */
    private String collectedContent;

    /**
     * 比对状态：1-成功，2-失败，3-有差异
     */
    private Integer status;

    /**
     * 差异数量
     */
    private Integer diffCount;

    /**
     * 差异详情（JSON格式）
     */
    private String diffDetails;

    /**
     * 差异百分比
     */
    private Double diffPercentage;

    /**
     * 比对耗时（毫秒）
     */
    private Long compareTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updateTime;
}