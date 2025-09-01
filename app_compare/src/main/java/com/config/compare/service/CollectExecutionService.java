package com.config.compare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CollectExecution;

import java.time.LocalDateTime;

/**
 * 采集执行记录Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CollectExecutionService extends IService<CollectExecution> {

    /**
     * 根据执行ID获取执行记录
     * 
     * @param executeId 执行ID
     * @return 执行记录
     */
    CollectExecution getByExecuteId(String executeId);

    /**
     * 删除过期的执行记录
     * 
     * @param cutoffTime 截止时间
     * @return 删除数量
     */
    int deleteExpiredRecords(LocalDateTime cutoffTime);
}