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
        
        // 根据差异级别计算权重分数
        double highWeight = 10.0; // 高级别差异权重
        double mediumWeight = 5.0; // 中级别差异权重
        double lowWeight = 1.0;   // 低级别差异权重
        
        double totalWeight = highDiffCount * highWeight + mediumDiffCount * mediumWeight + lowDiffCount * lowWeight;
        double maxWeight = totalItems * highWeight;
        
        double score = Math.max(0, (maxWeight - totalWeight) / maxWeight * 100);
        this.consistencyScore = BigDecimal.valueOf(score).setScale(2, BigDecimal.ROUND_HALF_UP);
        
        this.consistent = this.consistencyScore.compareTo(BigDecimal.valueOf(95)) >= 0;
    }

    /**
     * 设置执行耗时
     */
    public void setDuration(long startTimeMs) {
        this.durationMs = System.currentTimeMillis() - startTimeMs;
    }
}