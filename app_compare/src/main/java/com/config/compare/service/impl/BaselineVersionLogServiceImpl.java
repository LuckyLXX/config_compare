package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.BaselineVersionLog;
import com.config.compare.mapper.BaselineVersionLogMapper;
import com.config.compare.service.BaselineVersionLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 基线版本切换日志Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Service
public class BaselineVersionLogServiceImpl extends ServiceImpl<BaselineVersionLogMapper, BaselineVersionLog> implements BaselineVersionLogService {

    @Override
    public boolean recordSwitch(BaselineVersionLog versionLog) {
        try {
            boolean success = this.save(versionLog);
            if (success) {
                log.info("记录版本切换日志: {} -> {}, 操作类型: {}", 
                    versionLog.getOldVersion(), versionLog.getNewVersion(), versionLog.getOperationType());
            }
            return success;
        } catch (Exception e) {
            log.error("记录版本切换日志失败", e);
            throw new RuntimeException("记录版本切换日志失败: " + e.getMessage());
        }
    }

    @Override
    public List<BaselineVersionLog> getSwitchHistory(Long systemId, Long serverTypeId, Long categoryId) {
        LambdaQueryWrapper<BaselineVersionLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaselineVersionLog::getSystemId, systemId)
                   .eq(BaselineVersionLog::getServerTypeId, serverTypeId)
                   .eq(BaselineVersionLog::getCategoryId, categoryId)
                   .orderByDesc(BaselineVersionLog::getCreateTime);
        
        return this.list(queryWrapper);
    }

    @Override
    public List<BaselineVersionLog> getRecentSwitchHistory(Long systemId, Long serverTypeId, Long categoryId, int limit) {
        LambdaQueryWrapper<BaselineVersionLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaselineVersionLog::getSystemId, systemId)
                   .eq(BaselineVersionLog::getServerTypeId, serverTypeId)
                   .eq(BaselineVersionLog::getCategoryId, categoryId)
                   .orderByDesc(BaselineVersionLog::getCreateTime)
                   .last("LIMIT " + limit);
        
        return this.list(queryWrapper);
    }
}

