package com.config.compare.service;

import com.config.compare.entity.CollectTask;
import com.config.compare.entity.CompareTask;

import java.util.List;

/**
 * 定时任务调度服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface TaskSchedulerService {

    /**
     * 注册采集任务定时执行
     * 
     * @param task 采集任务
     */
    void scheduleCollectTask(CollectTask task);

    /**
     * 注册比对任务定时执行
     * 
     * @param task 比对任务
     */
    void scheduleCompareTask(CompareTask task);

    /**
     * 取消采集任务定时执行
     * 
     * @param taskId 任务ID
     */
    void unscheduleCollectTask(Long taskId);

    /**
     * 取消比对任务定时执行
     * 
     * @param taskId 任务ID
     */
    void unscheduleCompareTask(Long taskId);

    /**
     * 更新采集任务定时执行
     * 
     * @param task 采集任务
     */
    void updateCollectTaskSchedule(CollectTask task);

    /**
     * 更新比对任务定时执行
     * 
     * @param task 比对任务
     */
    void updateCompareTaskSchedule(CompareTask task);

    /**
     * 获取所有定时采集任务
     * 
     * @return 定时采集任务列表
     */
    List<CollectTask> getScheduledCollectTasks();

    /**
     * 获取所有定时比对任务
     * 
     * @return 定时比对任务列表
     */
    List<CompareTask> getScheduledCompareTasks();

    /**
     * 验证Cron表达式
     * 
     * @param cronExpression Cron表达式
     * @return 是否有效
     */
    boolean validateCronExpression(String cronExpression);

    /**
     * 获取下次执行时间
     * 
     * @param cronExpression Cron表达式
     * @return 下次执行时间
     */
    String getNextExecutionTime(String cronExpression);
}
