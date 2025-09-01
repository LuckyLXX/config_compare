package com.config.compare.compare.algorithm;

import com.config.compare.compare.model.CompareContext;
import com.config.compare.compare.model.CompareResultModel;

/**
 * 比对算法接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CompareAlgorithm {

    /**
     * 获取算法类型
     * 
     * @return 算法类型
     */
    String getAlgorithmType();

    /**
     * 获取算法名称
     * 
     * @return 算法名称
     */
    String getAlgorithmName();

    /**
     * 执行比对
     * 
     * @param context 比对上下文
     * @return 比对结果
     */
    CompareResultModel compare(CompareContext context);

    /**
     * 检查是否支持该类型的比对
     * 
     * @param contentType 内容类型
     * @return 是否支持
     */
    boolean supports(String contentType);

    /**
     * 获取算法描述
     * 
     * @return 描述信息
     */
    String getDescription();
}