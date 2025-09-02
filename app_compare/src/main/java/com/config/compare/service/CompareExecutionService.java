package com.config.compare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CompareExecution;

/**
 * 比对执行服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CompareExecutionService extends IService<CompareExecution> {

    /**
     * 根据执行ID获取执行记录
     * 
     * @param executeId 执行ID
     * @return 执行记录
     */
    CompareExecution getByExecuteId(String executeId);

    /**
     * 根据任务ID获取最新的执行记录
     * 
     * @param taskId 任务ID
     * @return 执行记录
     */
    CompareExecution getLatestByTaskId(Long taskId);
}
