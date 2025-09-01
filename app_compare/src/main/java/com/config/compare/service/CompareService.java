package com.config.compare.service;

import com.config.compare.compare.model.CompareContext;
import com.config.compare.compare.model.CompareResultModel;
import com.config.compare.entity.CompareExecution;
import com.config.compare.entity.CompareResult;
import com.config.compare.entity.CompareTask;

import java.util.List;

/**
 * 比对服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CompareService {

    /**
     * 执行比对任务
     * 
     * @param taskId 任务ID
     * @return 执行结果
     */
    boolean executeCompareTask(Long taskId);

    /**
     * 立即执行比对任务
     * 
     * @param task 比对任务
     * @return 执行ID
     */
    String executeTaskImmediately(CompareTask task);

    /**
     * 执行单个比对
     * 
     * @param context 比对上下文
     * @return 比对结果
     */
    CompareResultModel executeSingleCompare(CompareContext context);

    /**
     * 智能比对（自动选择算法）
     * 
     * @param context 比对上下文
     * @return 比对结果
     */
    CompareResultModel smartCompare(CompareContext context);

    /**
     * 指定算法比对
     * 
     * @param context 比对上下文
     * @param algorithmType 算法类型
     * @return 比对结果
     */
    CompareResultModel compareWithAlgorithm(CompareContext context, String algorithmType);

    /**
     * 获取执行状态
     * 
     * @param executeId 执行ID
     * @return 执行记录
     */
    CompareExecution getExecutionStatus(String executeId);

    /**
     * 获取执行结果
     * 
     * @param executeId 执行ID
     * @return 结果列表
     */
    List<CompareResult> getExecutionResults(String executeId);

    /**
     * 保存比对结果
     * 
     * @param taskId 任务ID
     * @param executeId 执行ID
     * @param baselineId 基线ID
     * @param serverInstanceId 服务器实例ID
     * @param collectResultId 采集结果ID
     * @param compareResult 比对结果
     * @return 保存结果
     */
    boolean saveCompareResult(Long taskId, String executeId, Long baselineId, Long serverInstanceId, 
                             Long collectResultId, CompareResultModel compareResult);

    /**
     * 创建执行记录
     * 
     * @param taskId 任务ID
     * @param executeId 执行ID
     * @param baselineId 基线ID
     * @param baselineVersion 基线版本
     * @param totalServers 总服务器数
     * @return 创建结果
     */
    boolean createExecutionRecord(Long taskId, String executeId, Long baselineId, String baselineVersion, int totalServers);

    /**
     * 更新执行记录
     * 
     * @param executeId 执行ID
     * @param status 状态
     * @param consistentCount 一致数量
     * @param inconsistentCount 不一致数量
     * @param failedCount 失败数量
     * @param overallScore 整体评分
     * @return 更新结果
     */
    boolean updateExecutionRecord(String executeId, Integer status, Integer consistentCount, 
                                 Integer inconsistentCount, Integer failedCount, java.math.BigDecimal overallScore);

    /**
     * 完成执行记录
     * 
     * @param executeId 执行ID
     * @return 完成结果
     */
    boolean finishExecutionRecord(String executeId);

    /**
     * 停止正在执行的比对
     * 
     * @param executeId 执行ID
     * @return 停止结果
     */
    boolean stopExecution(String executeId);

    /**
     * 获取支持的比对算法
     * 
     * @return 算法类型列表
     */
    List<String> getSupportedAlgorithms();

    /**
     * 清理过期的比对记录
     * 
     * @param retentionDays 保留天数
     * @return 清理结果
     */
    boolean cleanupExpiredRecords(int retentionDays);
}