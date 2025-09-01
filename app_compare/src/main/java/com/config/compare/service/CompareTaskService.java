package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CompareTask;

public interface CompareTaskService extends IService<CompareTask> {
    
    /**
     * 分页查询比对任务
     */
    IPage<CompareTask> pageQuery(int current, int size, String taskName, Long systemId, Integer executeType, Integer status);
    
    /**
     * 创建比对任务
     */
    boolean createTask(CompareTask task);
    
    /**
     * 更新比对任务
     */
    boolean updateTask(CompareTask task);
    
    /**
     * 删除比对任务
     */
    boolean deleteTask(Long id);
    
    /**
     * 启用/禁用比对任务
     */
    boolean toggleTaskStatus(Long id, Integer status);
    
    /**
     * 立即执行比对任务
     */
    String executeTask(Long id);
    
    /**
     * 批量执行比对任务
     */
    boolean batchExecute(Long[] taskIds);
}


