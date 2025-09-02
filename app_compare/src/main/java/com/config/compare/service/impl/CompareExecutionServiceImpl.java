package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CompareExecution;
import com.config.compare.mapper.CompareExecutionMapper;
import com.config.compare.service.CompareExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 比对执行服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompareExecutionServiceImpl extends ServiceImpl<CompareExecutionMapper, CompareExecution> implements CompareExecutionService {

    @Override
    public CompareExecution getByExecuteId(String executeId) {
        LambdaQueryWrapper<CompareExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompareExecution::getExecuteId, executeId);
        return this.getOne(wrapper);
    }

    @Override
    public CompareExecution getLatestByTaskId(Long taskId) {
        LambdaQueryWrapper<CompareExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompareExecution::getTaskId, taskId)
               .orderByDesc(CompareExecution::getCreateTime)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
