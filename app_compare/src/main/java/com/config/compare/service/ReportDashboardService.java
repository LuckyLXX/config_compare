package com.config.compare.service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 报告仪表板服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ReportDashboardService {

    /**
     * 获取仪表板概览数据
     * 
     * @param timeRange 时间范围
     * @return 概览数据
     */
    Map<String, Object> getDashboardOverview(String timeRange);

    /**
     * 获取仪表板概览数据（按时间范围）
     */
    Map<String, Object> getDashboardOverview(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取系统状态统计
     * 
     * @param systemId 系统ID
     * @return 系统状态统计
     */
    Map<String, Object> getSystemStats(Long systemId);

    /**
     * 获取任务执行趋势
     * 
     * @param timeRange 时间范围
     * @return 任务执行趋势数据
     */
    Map<String, Object> getTaskTrends(String timeRange);

    /**
     * 获取任务执行趋势（按时间范围与周期）
     */
    Map<String, Object> getTaskTrends(LocalDateTime startTime, LocalDateTime endTime, String period);

    /**
     * 获取比对结果分布
     * 
     * @param timeRange 时间范围
     * @return 比对结果分布数据
     */
    Map<String, Object> getCompareDistribution(String timeRange);

    /**
     * 获取比对结果分布（按时间范围）
     */
    Map<String, Object> getCompareDistribution(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取最近执行记录
     * 
     * @param limit 数量限制
     * @return 最近执行记录
     */
    Map<String, Object> getRecentExecutions(Integer limit);

    /**
     * 获取最近执行记录（按时间范围）
     */
    Map<String, Object> getRecentExecutions(LocalDateTime startTime, LocalDateTime endTime, Integer limit);

    /**
     * 获取告警信息
     * 
     * @param level 告警级别
     * @return 告警信息
     */
    Map<String, Object> getAlerts(String level);
}
