package com.config.compare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 比对差异详情实体
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("compare_diff_detail")
public class CompareDiffDetail {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 比对结果ID
     */
    private Long resultId;

    /**
     * 差异类型：ADD/DELETE/MODIFY
     */
    private String diffType;

    /**
     * 差异键
     */
    private String diffKey;

    /**
     * 差异级别：HIGH/MEDIUM/LOW
     */
    private String diffLevel;

    /**
     * 差异分类
     */
    private String diffCategory;

    /**
     * 差异描述
     */
    private String description;

    /**
     * 基线值
     */
    private String baselineValue;

    /**
     * 当前值
     */
    private String currentValue;

    /**
     * 建议操作
     */
    private String suggestAction;

    /**
     * 差异路径
     */
    private String diffPath;
    
    /**
     * 基线行号
     */
    // private Integer baselineLineNumber;
    
    /**
     * 当前行号
     */
    // private Integer currentLineNumber;
    
    /**
     * 行内差异信息（JSON格式）
     */
    // private String inlineDiffJson;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
