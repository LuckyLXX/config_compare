package com.config.compare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CompareResult;

import java.util.List;

/**
 * 比对结果服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CompareResultService extends IService<CompareResult> {

    /**
     * 根据执行ID获取比对结果列表
     * 
     * @param executeId 执行ID
     * @return 比对结果列表
     */
    List<CompareResult> getByExecuteId(String executeId);

    /**
     * 根据任务ID获取比对结果列表
     * 
     * @param taskId 任务ID
     * @return 比对结果列表
     */
    List<CompareResult> getByTaskId(Long taskId);

    /**
     * 根据服务器实例ID获取比对结果列表
     * 
     * @param serverInstanceId 服务器实例ID
     * @return 比对结果列表
     */
    List<CompareResult> getByServerInstanceId(Long serverInstanceId);
}
