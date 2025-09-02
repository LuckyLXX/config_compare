package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CompareResult;
import com.config.compare.mapper.CompareResultMapper;
import com.config.compare.service.CompareResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

/**
 * 比对结果服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompareResultServiceImpl extends ServiceImpl<CompareResultMapper, CompareResult> implements CompareResultService {

    @Override
    public List<CompareResult> getByExecuteId(String executeId) {
        LambdaQueryWrapper<CompareResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompareResult::getExecuteId, executeId)
               .orderByDesc(CompareResult::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<CompareResult> getByTaskId(Long taskId) {
        LambdaQueryWrapper<CompareResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompareResult::getTaskId, taskId)
               .orderByDesc(CompareResult::getCreateTime);
        return this.list(wrapper);
    }

    @Override
    public List<CompareResult> getByServerInstanceId(Long serverInstanceId) {
        LambdaQueryWrapper<CompareResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompareResult::getServerInstanceId, serverInstanceId)
               .orderByDesc(CompareResult::getCreateTime);
        return this.list(wrapper);
    }
}
