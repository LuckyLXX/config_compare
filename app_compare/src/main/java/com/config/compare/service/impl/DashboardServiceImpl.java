package com.config.compare.service.impl;

import com.config.compare.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 仪表板Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-08-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SystemInfoService systemInfoService;
    private final ServerInstanceService serverInstanceService;
    private final CollectTaskService collectTaskService;
    private final ConfigBaselineService configBaselineService;

    @Override
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 系统总数
            long systemCount = systemInfoService.count();
            stats.put("systemCount", systemCount);
            
            // 服务器总数
            long serverCount = serverInstanceService.count();
            stats.put("serverCount", serverCount);
            
            // 任务总数（采集任务数量）
            long taskCount = collectTaskService.count();
            stats.put("taskCount", taskCount);
            
            // 基线总数
            long baselineCount = configBaselineService.count();
            stats.put("reportCount", baselineCount);
            
            // 今日执行任务数（模拟数据，后续可以从执行记录表获取）
            stats.put("todayExecutions", 156);
            
            // 成功率（模拟数据，后续可以从执行记录表计算）
            stats.put("successRate", 92.5);
            
            // 活跃系统数（状态为启用的系统）
            // 这里暂时使用系统总数，后续可以根据实际需求调整
            stats.put("activeSystemCount", systemCount);
            
            // 最近7天趋势数据（模拟数据，后续可以从执行记录表获取）
            stats.put("weeklyTrend", new int[]{45, 52, 48, 61, 55, 67, 73});
            
            log.info("获取仪表板统计数据成功: {}", stats);
            
        } catch (Exception e) {
            log.error("获取仪表板统计数据失败", e);
            // 返回默认值
            stats.put("systemCount", 0);
            stats.put("serverCount", 0);
            stats.put("taskCount", 0);
            stats.put("reportCount", 0);
            stats.put("todayExecutions", 0);
            stats.put("successRate", 0);
            stats.put("activeSystemCount", 0);
            stats.put("weeklyTrend", new int[]{0, 0, 0, 0, 0, 0, 0});
        }
        
        return stats;
    }
}
