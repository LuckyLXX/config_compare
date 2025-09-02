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
    private String executeId;

    /**
     * 基线ID
     */
    private Long baselineId;

    /**
     * 服务器实例ID
     */
    private Long serverInstanceId;

    /**
     * 采集结果ID
     */
    private Long collectResultId;

    /**
     * 比对状态：1一致 0不一致 -1比对失败
     */
    private Integer compareStatus;

    /**
     * 一致性评分
     */
    private java.math.BigDecimal consistencyScore;

    /**
     * 差异数量
     */
    private Integer diffCount;

    /**
     * 高级别差异数量
     */
    private Integer highDiffCount;

    /**
     * 中级别差异数量
     */
    private Integer mediumDiffCount;

    /**
     * 低级别差异数量
     */
    private Integer lowDiffCount;

    /**
     * 差异摘要JSON
     */
    private String diffSummary;

    /**
     * 执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime executeTime;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

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