package com.config.compare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CollectResultEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集结果Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CollectResultEntityService extends IService<CollectResultEntity> {

    /**
     * 根据执行ID获取结果列表
     * 
     * @param executeId 执行ID
     * @return 结果列表
     */
    List<CollectResultEntity> listByExecuteId(String executeId);

    /**
     * 删除过期的采集结果
     * 
     * @param cutoffTime 截止时间
     * @return 删除数量
     */
    int deleteExpiredResults(LocalDateTime cutoffTime);
}