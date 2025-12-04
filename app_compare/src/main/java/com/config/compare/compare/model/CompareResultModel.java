package com.config.compare.compare.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 比对结果模型
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class CompareResultModel {

    /**
     * 比对是否成功
     */
    private boolean success;

    /**
     * 是否一致
     */
    private boolean consistent;

    /**
     * 一致性评分（0-100）
     */
    private BigDecimal consistencyScore;

    /**
     * 差异数量
     */
    private int diffCount;

    /**
     * 高级别差异数量
     */
    private int highDiffCount;

    /**
     * 中级别差异数量
     */
    private int mediumDiffCount;

    /**
     * 低级别差异数量
     */
    private int lowDiffCount;

    /**
     * 差异详情列表
     */
    private List<DiffItem> diffItems;

    /**
     * 完整的对齐行信息（用于前端显示）
     */
    private List<AlignedLine> alignedLines;

    /**
     * 差异摘要
     */
    private String diffSummary;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 算法类型
     */
    private String algorithmType;

    public CompareResultModel() {
        this.executeTime = LocalDateTime.now();
        this.diffItems = new ArrayList<>();
        this.consistencyScore = BigDecimal.ZERO;
    }

    public CompareResultModel(boolean success) {
        this();
        this.success = success;
    }

    public CompareResultModel(boolean success, boolean consistent) {
        this(success);
        this.consistent = consistent;
    }

    /**
     * 创建成功的比对结果
     */
    public static CompareResultModel success(boolean consistent) {
        return new CompareResultModel(true, consistent);
    }

    /**
     * 创建失败的比对结果
     */
    public static CompareResultModel fail(String errorMessage) {
        CompareResultModel result = new CompareResultModel(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 添加差异项
     */
    public void addDiffItem(DiffItem diffItem) {
        if (diffItem != null) {
            this.diffItems.add(diffItem);
            this.diffCount++;
            
            switch (diffItem.getDiffLevel()) {
                case "HIGH":
                    this.highDiffCount++;
                    break;
                case "MEDIUM":
                    this.mediumDiffCount++;
                    break;
                case "LOW":
                    this.lowDiffCount++;
                    break;
            }
        }
    }

    /**
     * 计算一致性评分
     */
    public void calculateConsistencyScore(int totalItems) {
        if (totalItems <= 0) {
            this.consistencyScore = BigDecimal.valueOf(100);
            return;
        }
        
        // 如果没有差异，100%一致
        if (diffCount == 0) {
            this.consistencyScore = BigDecimal.valueOf(100);
            this.consistent = true;
            return;
        }
        
        // 有差异时，根据差异数量和级别计算评分
        // 使用差异占比计算，确保有差异时不会是100%
        double highWeight = 3.0;   // 高级别差异权重
        double mediumWeight = 2.0; // 中级别差异权重
        double lowWeight = 1.0;    // 低级别差异权重
        
        double weightedDiffCount = highDiffCount * highWeight + mediumDiffCount * mediumWeight + lowDiffCount * lowWeight;
        
        // 使用对数衰减，让差异影响更明显
        double diffRatio = weightedDiffCount / totalItems;
        double score = Math.max(0, (1 - diffRatio) * 100);
        
        // 确保有差异时最高99.99%
        score = Math.min(score, 99.99);
        
        this.consistencyScore = BigDecimal.valueOf(score).setScale(2, BigDecimal.ROUND_HALF_UP);
        this.consistent = false;
    }

    /**
     * 设置执行耗时
     */
    public void setDuration(long startTimeMs) {
        this.durationMs = System.currentTimeMillis() - startTimeMs;
    }
}