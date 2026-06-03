package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比对规则实体类
 */
@Data
@TableName("compare_rule")
public class CompareRule implements Serializable {

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
     * 比对类型
     */
    private String compareType;

    /**
     * SSH文本比对方式
     */
    private String sshCompareMode;

    /**
     * 忽略行关键词
     */
    private String ignoreLines;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * 文件比对选项（JSON格式）
     */
    private String fileCompareOptions;

    /**
     * Apollo比对模式
     */
    private String apolloCompareMode;

    /**
     * 忽略字段
     */
    private String ignoreFields;

    /**
     * 敏感字段
     */
    private String sensitiveFields;

    /**
     * YAML比对选项（JSON格式）
     */
    private String yamlCompareOptions;

    /**
     * 环境变量比对方式
     */
    private String envCompareMode;

    /**
     * 环境变量模板
     */
    private String envTemplate;

    /**
     * 自定义规则（JSON格式）
     */
    private String customRules;

    /**
     * 差异阈值
     */
    private Double diffThreshold;

    /**
     * 忽略差异选项（JSON格式）
     */
    private String ignoreDifferences;

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

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 更新人
     */
    private String updateBy;
}


























