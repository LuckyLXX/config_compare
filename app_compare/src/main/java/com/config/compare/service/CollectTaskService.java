package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CollectTask;

import java.util.List;
import java.util.Map;

/**
 * 采集任务Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CollectTaskService extends IService<CollectTask> {

    /**
     * 分页查询采集任务列表
     */
    IPage<Map<String, Object>> getTaskListWithDetails(int current, int size, String taskName, Long systemId, Integer executeType);

    /**
     * 根据ID获取任务详情（包含关联信息）
     */
    Map<String, Object> getTaskWithDetails(Long id);

    /**
     * 创建采集任务
     */
    boolean createTask(CollectTask task);

    /**
     * 更新采集任务
     */
    boolean updateTask(CollectTask task);

    /**
     * 启用/禁用任务
     */
    boolean toggleTaskStatus(Long id, Integer status);

    /**
     * 立即执行任务
     */
    String executeTask(Long id);

    /**
     * 批量执行任务
     */
    boolean batchExecute(List<Long> taskIds);

    /**
     * 暂停任务
     */
    boolean pauseTask(Long id);

    /**
     * 恢复任务
     */
    boolean resumeTask(Long id);

    /**
     * 获取任务执行历史
     */
    IPage<Map<String, Object>> getTaskExecutions(Long taskId, int current, int size);

    /**
     * 获取任务执行结果
     */
    IPage<Map<String, Object>> getTaskResults(Long taskId, String executionId, int current, int size);
}