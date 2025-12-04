package com.config.compare.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.BaselineVersionLog;

import java.util.List;

/**
 * 基线版本切换日志Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-04
 */
public interface BaselineVersionLogService extends IService<BaselineVersionLog> {

    /**
     * 记录版本切换日志
     * 
     * @param log 切换日志信息
     * @return 是否记录成功
     */
    boolean recordSwitch(BaselineVersionLog log);
    
    /**
     * 获取指定分组的切换历史
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @return 切换历史列表
     */
    List<BaselineVersionLog> getSwitchHistory(Long systemId, Long serverTypeId, Long categoryId);
    
    /**
     * 获取最近N条切换记录
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @param limit 限制条数
     * @return 切换历史列表
     */
    List<BaselineVersionLog> getRecentSwitchHistory(Long systemId, Long serverTypeId, Long categoryId, int limit);
}

