package com.config.compare.service;

import java.util.Map;

/**
 * 仪表板Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-08-26
 */
public interface DashboardService {

    /**
     * 获取仪表板统计数据
     * 
     * @return 统计数据
     */
    Map<String, Object> getDashboardStats();
}
