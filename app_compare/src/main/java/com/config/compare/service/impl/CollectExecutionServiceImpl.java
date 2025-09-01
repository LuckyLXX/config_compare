package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CollectExecution;
import com.config.compare.mapper.CollectExecutionMapper;
import com.config.compare.service.CollectExecutionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 采集执行记录Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Service
public class CollectExecutionServiceImpl extends ServiceImpl<CollectExecutionMapper, CollectExecution> implements CollectExecutionService {

    @Override
    public CollectExecution getByExecuteId(String executeId) {
        LambdaQueryWrapper<CollectExecution> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectExecution::getExecuteId, executeId);
        return this.getOne(queryWrapper);
    }

    @Override
    public int deleteExpiredRecords(LocalDateTime cutoffTime) {
        LambdaQueryWrapper<CollectExecution> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(CollectExecution::getStartTime, cutoffTime);
        return this.baseMapper.delete(queryWrapper);
    }
}