package com.config.compare.service;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.config.compare.entity.CollectExecution;
import com.config.compare.entity.CollectResultEntity;
import com.config.compare.entity.CollectTask;

import java.util.List;
import java.util.Map;

/**
 * 采集服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CollectService {

    /**
     * 执行单个采集任务
     * 
     * @param taskId 任务ID
     * @return 执行结果
     */
    boolean executeTask(Long taskId);

    /**
     * 立即执行采集任务
     * 
     * @param task 采集任务
     * @return 执行ID
     */
    String executeTaskImmediately(CollectTask task);

    /**
     * 执行单个服务器的采集
     * 
     * @param context 采集上下文
     * @return 采集结果
     */
    CollectResult executeSingleCollect(CollectContext context);

    /**
     * 批量执行多个服务器的采集
     * 
     * @param contexts 采集上下文列表
     * @return 执行结果映射
     */
    Map<Long, CollectResult> executeBatchCollect(List<CollectContext> contexts);

    /**
     * 测试采集配置
     * 
     * @param context 采集上下文
     * @return 测试结果
     */
    boolean testCollectConfig(CollectContext context);

    /**
     * 停止正在执行的任务
     * 
     * @param executeId 执行ID
     * @return 停止结果
     */
    boolean stopExecution(String executeId);

    /**
     * 获取执行状态
     * 
     * @param executeId 执行ID
     * @return 执行记录
     */
    CollectExecution getExecutionStatus(String executeId);

    /**
     * 获取执行结果
     * 
     * @param executeId 执行ID
     * @return 结果列表
     */
    List<CollectResultEntity> getExecutionResults(String executeId);

    /**
     * 保存采集结果
     * 
     * @param taskId 任务ID
     * @param executeId 执行ID
     * @param serverInstanceId 服务器实例ID
     * @param result 采集结果
     * @return 保存结果
     */
    boolean saveCollectResult(Long taskId, String executeId, Long serverInstanceId, CollectResult result);

    /**
     * 创建执行记录
     * 
     * @param taskId 任务ID
     * @param executeId 执行ID
     * @param totalServers 总服务器数
     * @return 创建结果
     */
    boolean createExecutionRecord(Long taskId, String executeId, int totalServers);

    /**
     * 更新执行记录状态
     * 
     * @param executeId 执行ID
     * @param status 状态
     * @param successCount 成功数量
     * @param failedCount 失败数量
     * @param errorMessage 错误信息
     * @return 更新结果
     */
    boolean updateExecutionRecord(String executeId, Integer status, Integer successCount, 
                                 Integer failedCount, String errorMessage);

    /**
     * 完成执行记录
     * 
     * @param executeId 执行ID
     * @return 完成结果
     */
    boolean finishExecutionRecord(String executeId);

    /**
     * 清理过期的执行记录和结果
     * 
     * @param retentionDays 保留天数
     * @return 清理结果
     */
    boolean cleanupExpiredRecords(int retentionDays);
}