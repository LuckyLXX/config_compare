package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比对执行实体类
 */
@Data
@TableName("compare_execution")
public class CompareExecution implements Serializable {

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
     * 执行ID
     */
    private String executeId;

    /**
     * 使用的基线ID
     */
    private Long baselineId;

    /**
     * 基线版本
     */
    private String baselineVersion;

    /**
     * 执行状态：1-执行中，2-执行成功，3-执行失败
     */
    private Integer executeStatus;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 总服务器数
     */
    private Integer totalServers;

    /**
     * 一致服务器数
     */
    private Integer consistentServers;

    /**
     * 不一致服务器数
     */
    private Integer inconsistentServers;

    /**
     * 比对失败服务器数
     */
    private Integer failedServers;

    /**
     * 整体一致性评分
     */
    private java.math.BigDecimal overallScore;

    /**
     * 执行人
     */
    private String executeBy;

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