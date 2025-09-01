package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CollectResultEntity;
import com.config.compare.mapper.CollectResultEntityMapper;
import com.config.compare.service.CollectResultEntityService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 采集结果Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Service
public class CollectResultEntityServiceImpl extends ServiceImpl<CollectResultEntityMapper, CollectResultEntity> implements CollectResultEntityService {

    @Override
    public List<CollectResultEntity> listByExecuteId(String executeId) {
        LambdaQueryWrapper<CollectResultEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectResultEntity::getExecuteId, executeId)
                   .orderByDesc(CollectResultEntity::getExecuteTime);
        return this.list(queryWrapper);
    }

    @Override
    public int deleteExpiredResults(LocalDateTime cutoffTime) {
        LambdaQueryWrapper<CollectResultEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.lt(CollectResultEntity::getExecuteTime, cutoffTime);
        return this.baseMapper.delete(queryWrapper);
    }
}