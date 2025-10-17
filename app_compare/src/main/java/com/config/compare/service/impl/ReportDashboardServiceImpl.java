package com.config.compare.service.impl;

import com.config.compare.entity.*;
import com.config.compare.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 报告仪表板服务实现
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportDashboardServiceImpl implements ReportDashboardService {

    private final SystemInfoService systemInfoService;
    private final ServerInstanceService serverInstanceService;
    private final CollectTaskService collectTaskService;
    private final CompareTaskService compareTaskService;
    private final CollectExecutionService collectExecutionService;
    private final CompareExecutionService compareExecutionService;
    private final CompareResultService compareResultService;

    @Override
    public Map<String, Object> getDashboardOverview(String timeRange) {
        try {
            // 获取系统数量
            long totalSystems = systemInfoService.count();
            long onlineSystems = systemInfoService.lambdaQuery()
                .eq(SystemInfo::getStatus, 1)
                .count();

            // 获取任务数量
            long totalCollectTasks = collectTaskService.count();
            long totalCompareTasks = compareTaskService.count();
            long totalTasks = totalCollectTasks + totalCompareTasks;

            // 获取运行中的任务数量（最近24小时有执行的）
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            long runningCollectTasks = collectExecutionService.lambdaQuery()
                .ge(CollectExecution::getStartTime, yesterday)
                .eq(CollectExecution::getExecuteStatus, 4) // 运行中
                .count();
            long runningCompareTasks = compareExecutionService.lambdaQuery()
                .ge(CompareExecution::getStartTime, yesterday)
                .eq(CompareExecution::getExecuteStatus, 4) // 运行中
                .count();
            long runningTasks = runningCollectTasks + runningCompareTasks;

            // 计算成功率（最近7天）
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            long totalExecutions = collectExecutionService.lambdaQuery()
                .ge(CollectExecution::getStartTime, weekAgo)
                .count() + compareExecutionService.lambdaQuery()
                .ge(CompareExecution::getStartTime, weekAgo)
                .count();

            long successfulExecutions = collectExecutionService.lambdaQuery()
                .ge(CollectExecution::getStartTime, weekAgo)
                .eq(CollectExecution::getExecuteStatus, 1) // 成功
                .count() + compareExecutionService.lambdaQuery()
                .ge(CompareExecution::getStartTime, weekAgo)
                .eq(CompareExecution::getExecuteStatus, 1) // 成功
                .count();

            double successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0;

            Map<String, Object> result = new HashMap<>();
            result.put("systemCount", totalSystems);
            result.put("onlineSystemCount", onlineSystems);
            result.put("taskCount", totalTasks);
            result.put("runningTaskCount", runningTasks);
            result.put("successRate", Math.round(successRate * 100.0) / 100.0);
            result.put("lastUpdateTime", System.currentTimeMillis());
            
            // 添加前端期望的字段
            long todayExecCount = getTodayExecutionCount();
            double todaySuccessRate = getTodaySuccessRate();
            long compareResultCount = getCompareResultCount();
            double consistencyRate = getConsistencyRate();
            
            result.put("todayExecutionCount", todayExecCount);
            result.put("todaySuccessRate", todaySuccessRate);
            result.put("compareResultCount", compareResultCount);
            result.put("consistencyRate", consistencyRate);

            log.info("仪表板概览数据: 系统总数={}, 在线系统={}, 任务总数={}, 运行中任务={}, 今日执行={}, 今日成功率={}%, 比对结果数={}, 一致性率={}%", 
                totalSystems, onlineSystems, totalTasks, runningTasks, todayExecCount, todaySuccessRate, compareResultCount, consistencyRate);

            return result;
        } catch (Exception e) {
            log.error("获取仪表板概览数据失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getDashboardOverview(LocalDateTime startTime, LocalDateTime endTime) {
        // 复用无参版本的静态部分，但按时间范围统计比对相关指标
        Map<String, Object> base = getDashboardOverview((String) null);
        try {
            long compareResultCount = compareResultService.lambdaQuery()
                .ge(startTime != null, CompareResult::getExecuteTime, startTime)
                .le(endTime != null, CompareResult::getExecuteTime, endTime)
                .count();

            long consistent = compareResultService.lambdaQuery()
                .ge(startTime != null, CompareResult::getExecuteTime, startTime)
                .le(endTime != null, CompareResult::getExecuteTime, endTime)
                .eq(CompareResult::getCompareStatus, 1)
                .count();

            double consistencyRate = compareResultCount == 0 ? 0.0 :
                Math.round(((double) consistent / compareResultCount) * 100 * 100.0) / 100.0;

            base.put("compareResultCount", compareResultCount);
            base.put("consistencyRate", consistencyRate);
            return base;
        } catch (Exception e) {
            log.error("按时间范围获取仪表板概览数据失败", e);
            return base;
        }
    }

    @Override
    public Map<String, Object> getSystemStats(Long systemId) {
        try {
            List<Map<String, Object>> systemStats = new ArrayList<>();
            
            // 获取所有系统或指定系统
            List<SystemInfo> systems;
            if (systemId != null) {
                SystemInfo system = systemInfoService.getById(systemId);
                systems = system != null ? Arrays.asList(system) : new ArrayList<>();
            } else {
                systems = systemInfoService.list();
            }

            for (SystemInfo system : systems) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("systemName", system.getSystemName());
                stat.put("status", system.getStatus() != null && system.getStatus() == 1 ? "ONLINE" : "OFFLINE");
                
                // 获取该系统的服务器数量
                long serverCount = serverInstanceService.lambdaQuery()
                    .eq(ServerInstance::getSystemId, system.getId())
                    .count();
                stat.put("serverCount", serverCount);

                // 获取该系统的采集任务数量
                long collectCount = collectTaskService.lambdaQuery()
                    .eq(CollectTask::getSystemId, system.getId())
                    .count();
                stat.put("collectCount", collectCount);

                // 获取该系统的比对任务数量
                long compareCount = compareTaskService.lambdaQuery()
                    .eq(CompareTask::getSystemId, system.getId())
                    .count();
                stat.put("compareCount", compareCount);

                systemStats.add(stat);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", systemStats);
            result.put("total", systemStats.size());

            return result;
        } catch (Exception e) {
            log.error("获取系统状态统计失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getTaskTrends(String timeRange) {
        try {
            // 默认获取最近7天的数据
            int days = 7;
            if (StringUtils.hasText(timeRange)) {
                if (timeRange.equals("30d")) {
                    days = 30;
                } else if (timeRange.equals("90d")) {
                    days = 30; // 按月统计
                }
            }

            List<String> dates = new ArrayList<>();
            List<Integer> collectValues = new ArrayList<>();
            List<Integer> compareValues = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            
            for (int i = days - 1; i >= 0; i--) {
                LocalDateTime date = LocalDateTime.now().minusDays(i);
                String dateStr = date.format(formatter);
                dates.add(dateStr);

                // 统计当天的采集执行次数
                LocalDateTime startOfDay = date.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1);
                
                long collectCount = collectExecutionService.lambdaQuery()
                    .ge(CollectExecution::getStartTime, startOfDay)
                    .lt(CollectExecution::getStartTime, endOfDay)
                    .count();
                collectValues.add((int) collectCount);

                // 统计当天的比对执行次数
                long compareCount = compareExecutionService.lambdaQuery()
                    .ge(CompareExecution::getStartTime, startOfDay)
                    .lt(CompareExecution::getStartTime, endOfDay)
                    .count();
                compareValues.add((int) compareCount);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates.toArray(new String[0]));
            result.put("collectCounts", collectValues.toArray(new Integer[0]));
            result.put("compareCounts", compareValues.toArray(new Integer[0]));

            return result;
        } catch (Exception e) {
            log.error("获取任务执行趋势失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getTaskTrends(LocalDateTime startTime, LocalDateTime endTime, String period) {
        try {
            if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
                return getTaskTrends((String) null);
            }

            List<String> dates = new ArrayList<>();
            List<Integer> collectValues = new ArrayList<>();
            List<Integer> compareValues = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDateTime cursor = startTime.toLocalDate().atStartOfDay();
            LocalDateTime endDayStart = endTime.toLocalDate().atStartOfDay();
            int guard = 0;
            while (!cursor.isAfter(endDayStart) && guard < 370) { // 最多统计一年多，避免无限循环
                String dateStr = cursor.format(formatter);
                dates.add(dateStr);

                LocalDateTime startOfDay = cursor;
                LocalDateTime endOfDay = startOfDay.plusDays(1);

                long collectCount = collectExecutionService.lambdaQuery()
                    .ge(CollectExecution::getStartTime, startOfDay)
                    .lt(CollectExecution::getStartTime, endOfDay)
                    .count();
                collectValues.add((int) collectCount);

                long compareCount = compareExecutionService.lambdaQuery()
                    .ge(CompareExecution::getStartTime, startOfDay)
                    .lt(CompareExecution::getStartTime, endOfDay)
                    .count();
                compareValues.add((int) compareCount);

                cursor = cursor.plusDays(1);
                guard++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("dates", dates.toArray(new String[0]));
            result.put("collectCounts", collectValues.toArray(new Integer[0]));
            result.put("compareCounts", compareValues.toArray(new Integer[0]));
            return result;
        } catch (Exception e) {
            log.error("按时间范围获取任务执行趋势失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getCompareDistribution(String timeRange) {
        try {
            // 获取最近7天的比对结果分布（基于 compare_result 表）
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

            long consistentResults = compareResultService.lambdaQuery()
                .ge(CompareResult::getExecuteTime, weekAgo)
                .eq(CompareResult::getCompareStatus, 1) // 一致
                .count();

            long inconsistentResults = compareResultService.lambdaQuery()
                .ge(CompareResult::getExecuteTime, weekAgo)
                .eq(CompareResult::getCompareStatus, 0) // 不一致
                .count();

            long failedResults = compareResultService.lambdaQuery()
                .ge(CompareResult::getExecuteTime, weekAgo)
                .eq(CompareResult::getCompareStatus, -1) // 失败
                .count();

            Map<String, Object> result = new HashMap<>();
            result.put("consistent", consistentResults);
            result.put("inconsistent", inconsistentResults);
            result.put("failed", failedResults);

            return result;
        } catch (Exception e) {
            log.error("获取比对结果分布失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getCompareDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            long consistentResults = compareResultService.lambdaQuery()
                .ge(startTime != null, CompareResult::getExecuteTime, startTime)
                .le(endTime != null, CompareResult::getExecuteTime, endTime)
                .eq(CompareResult::getCompareStatus, 1)
                .count();

            long inconsistentResults = compareResultService.lambdaQuery()
                .ge(startTime != null, CompareResult::getExecuteTime, startTime)
                .le(endTime != null, CompareResult::getExecuteTime, endTime)
                .eq(CompareResult::getCompareStatus, 0)
                .count();

            long failedResults = compareResultService.lambdaQuery()
                .ge(startTime != null, CompareResult::getExecuteTime, startTime)
                .le(endTime != null, CompareResult::getExecuteTime, endTime)
                .eq(CompareResult::getCompareStatus, -1)
                .count();

            Map<String, Object> result = new HashMap<>();
            result.put("consistent", consistentResults);
            result.put("inconsistent", inconsistentResults);
            result.put("failed", failedResults);
            log.info("比对结果分布: start={}, end={}, 一致={}, 不一致={}, 失败={}", startTime, endTime, consistentResults, inconsistentResults, failedResults);
            return result;
        } catch (Exception e) {
            log.error("按时间范围获取比对结果分布失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getRecentExecutions(Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<Map<String, Object>> executions = new ArrayList<>();

            // 获取最近的采集执行记录
            List<CollectExecution> collectExecutions = collectExecutionService.lambdaQuery()
                .orderByDesc(CollectExecution::getStartTime)
                .last("LIMIT " + limit)
                .list();

            for (CollectExecution execution : collectExecutions) {
                CollectTask task = collectTaskService.getById(execution.getTaskId());
                Map<String, Object> exec = new HashMap<>();
                exec.put("taskName", task != null ? task.getTaskName() : "未知任务");
                exec.put("taskType", "COLLECT");
                exec.put("status", getExecutionStatusCode(execution.getExecuteStatus()));
                exec.put("executeTime", formatTime(execution.getStartTime()));
                exec.put("duration", formatDuration(execution.getStartTime(), execution.getEndTime(), execution.getDurationMs()));
                executions.add(exec);
            }

            // 获取最近的比对执行记录
            List<CompareExecution> compareExecutions = compareExecutionService.lambdaQuery()
                .orderByDesc(CompareExecution::getStartTime)
                .last("LIMIT " + limit)
                .list();

            for (CompareExecution execution : compareExecutions) {
                CompareTask task = compareTaskService.getById(execution.getTaskId());
                Map<String, Object> exec = new HashMap<>();
                exec.put("taskName", task != null ? task.getTaskName() : "未知任务");
                exec.put("taskType", "COMPARE");
                exec.put("status", determineCompareStatusCode(execution));
                exec.put("executeTime", formatTime(execution.getStartTime()));
                exec.put("duration", formatDuration(execution.getStartTime(), execution.getEndTime(), execution.getDurationMs()));
                executions.add(exec);
            }

            // 按执行时间排序
            executions.sort((a, b) -> {
                String timeA = (String) a.get("executeTime");
                String timeB = (String) b.get("executeTime");
                return timeB.compareTo(timeA);
            });

            // 限制返回数量
            if (executions.size() > limit) {
                executions = executions.subList(0, limit);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", executions);
            result.put("total", executions.size());

            return result;
        } catch (Exception e) {
            log.error("获取最近执行记录失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getRecentExecutions(LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
        try {
            if (limit == null || limit <= 0) {
                limit = 10;
            }

            List<Map<String, Object>> executions = new ArrayList<>();

            List<CollectExecution> collectExecutions = collectExecutionService.lambdaQuery()
                .ge(startTime != null, CollectExecution::getStartTime, startTime)
                .le(endTime != null, CollectExecution::getStartTime, endTime)
                .orderByDesc(CollectExecution::getStartTime)
                .last("LIMIT " + limit)
                .list();

            for (CollectExecution execution : collectExecutions) {
                CollectTask task = collectTaskService.getById(execution.getTaskId());
                Map<String, Object> exec = new HashMap<>();
                exec.put("taskName", task != null ? task.getTaskName() : "未知任务");
                exec.put("taskType", "COLLECT");
                exec.put("status", getExecutionStatusCode(execution.getExecuteStatus()));
                exec.put("executeTime", formatTime(execution.getStartTime()));
                exec.put("duration", formatDuration(execution.getStartTime(), execution.getEndTime(), execution.getDurationMs()));
                executions.add(exec);
            }

            List<CompareExecution> compareExecutions = compareExecutionService.lambdaQuery()
                .ge(startTime != null, CompareExecution::getStartTime, startTime)
                .le(endTime != null, CompareExecution::getStartTime, endTime)
                .orderByDesc(CompareExecution::getStartTime)
                .last("LIMIT " + limit)
                .list();

            for (CompareExecution execution : compareExecutions) {
                CompareTask task = compareTaskService.getById(execution.getTaskId());
                Map<String, Object> exec = new HashMap<>();
                exec.put("taskName", task != null ? task.getTaskName() : "未知任务");
                exec.put("taskType", "COMPARE");
                exec.put("status", getExecutionStatusCode(execution.getExecuteStatus()));
                exec.put("executeTime", formatTime(execution.getStartTime()));
                exec.put("duration", formatDuration(execution.getStartTime(), execution.getEndTime(), execution.getDurationMs()));
                executions.add(exec);
            }

            executions.sort((a, b) -> {
                String timeA = (String) a.get("executeTime");
                String timeB = (String) b.get("executeTime");
                return timeB.compareTo(timeA);
            });

            if (executions.size() > limit) {
                executions = executions.subList(0, limit);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("records", executions);
            result.put("total", executions.size());
            return result;
        } catch (Exception e) {
            log.error("按时间范围获取最近执行记录失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public Map<String, Object> getAlerts(String level) {
        try {
            List<Map<String, Object>> alerts = new ArrayList<>();

            // 检查连接异常的服务器
            List<ServerInstance> offlineServers = serverInstanceService.lambdaQuery()
                .eq(ServerInstance::getConnectStatus, 0)
                .list();

            for (ServerInstance server : offlineServers) {
                Map<String, Object> alert = new HashMap<>();
                alert.put("level", "HIGH");
                alert.put("message", "服务器连接异常: " + server.getInstanceName());
                alert.put("time", server.getLastConnectTime() != null ? 
                    server.getLastConnectTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : 
                    "未知时间");
                alerts.add(alert);
            }

            // 检查最近失败的执行任务
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<CollectExecution> failedCollects = collectExecutionService.lambdaQuery()
                .ge(CollectExecution::getStartTime, yesterday)
                .eq(CollectExecution::getExecuteStatus, 3) // 失败
                .list();

            for (CollectExecution execution : failedCollects) {
                CollectTask task = collectTaskService.getById(execution.getTaskId());
                Map<String, Object> alert = new HashMap<>();
                alert.put("level", "MEDIUM");
                alert.put("message", "采集任务执行失败: " + (task != null ? task.getTaskName() : "未知任务"));
                alert.put("time", execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                alerts.add(alert);
            }

            List<CompareExecution> failedCompares = compareExecutionService.lambdaQuery()
                .ge(CompareExecution::getStartTime, yesterday)
                .eq(CompareExecution::getExecuteStatus, 3) // 失败
                .list();

            for (CompareExecution execution : failedCompares) {
                CompareTask task = compareTaskService.getById(execution.getTaskId());
                Map<String, Object> alert = new HashMap<>();
                alert.put("level", "MEDIUM");
                alert.put("message", "比对任务执行失败: " + (task != null ? task.getTaskName() : "未知任务"));
                alert.put("time", execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                alerts.add(alert);
            }

            // 按级别和时间排序
            alerts.sort((a, b) -> {
                String levelA = (String) a.get("level");
                String levelB = (String) b.get("level");
                if (!levelA.equals(levelB)) {
                    return levelA.equals("HIGH") ? -1 : 1;
                }
                String timeA = (String) a.get("time");
                String timeB = (String) b.get("time");
                return timeB.compareTo(timeA);
            });

            Map<String, Object> result = new HashMap<>();
            result.put("records", alerts);
            result.put("total", alerts.size());

            return result;
        } catch (Exception e) {
            log.error("获取告警信息失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 获取今日执行次数
     */
    private long getTodayExecutionCount() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        long collectCount = collectExecutionService.lambdaQuery()
            .ge(CollectExecution::getStartTime, today)
            .lt(CollectExecution::getStartTime, tomorrow)
            .count();
            
        long compareCount = compareExecutionService.lambdaQuery()
            .ge(CompareExecution::getStartTime, today)
            .lt(CompareExecution::getStartTime, tomorrow)
            .count();
            
        return collectCount + compareCount;
    }

    /**
     * 获取今日成功率
     */
    private double getTodaySuccessRate() {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1);
        
        long totalCount = collectExecutionService.lambdaQuery()
            .ge(CollectExecution::getStartTime, today)
            .lt(CollectExecution::getStartTime, tomorrow)
            .count() + compareExecutionService.lambdaQuery()
            .ge(CompareExecution::getStartTime, today)
            .lt(CompareExecution::getStartTime, tomorrow)
            .count();
            
        if (totalCount == 0) return 0.0;
        
        long successCount = collectExecutionService.lambdaQuery()
            .ge(CollectExecution::getStartTime, today)
            .lt(CollectExecution::getStartTime, tomorrow)
            .eq(CollectExecution::getExecuteStatus, 1)
            .count() + compareExecutionService.lambdaQuery()
            .ge(CompareExecution::getStartTime, today)
            .lt(CompareExecution::getStartTime, tomorrow)
            .eq(CompareExecution::getExecuteStatus, 1)
            .count();
            
        return Math.round((double) successCount / totalCount * 100 * 100.0) / 100.0;
    }

    /**
     * 获取比对结果数量
     */
    private long getCompareResultCount() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        // 统计最近7天内 compare_result 的记录数量
        return compareResultService.lambdaQuery()
            .ge(CompareResult::getExecuteTime, weekAgo)
            .count();
    }

    /**
     * 获取一致性率
     */
    private double getConsistencyRate() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        long totalResults = compareResultService.lambdaQuery()
            .ge(CompareResult::getExecuteTime, weekAgo)
            .count();

        if (totalResults == 0) return 0.0;

        long consistentResults = compareResultService.lambdaQuery()
            .ge(CompareResult::getExecuteTime, weekAgo)
            .eq(CompareResult::getCompareStatus, 1)
            .count();

        return Math.round((double) consistentResults / totalResults * 100 * 100.0) / 100.0;
    }

    /**
     * 获取执行状态文本
     */
    private String getExecutionStatusCode(Integer status) {
        if (status == null) return "UNKNOWN";
        switch (status) {
            case 1: return "SUCCESS";
            case 2: return "SUCCESS"; // 比对有不一致也视为任务成功完成
            case 3: return "FAILED";
            case 4: return "RUNNING";
            default: return "UNKNOWN";
        }
    }

    /**
     * 根据 compare_result 汇总来判定比对任务执行状态：
     * - 存在失败(-1) -> FAILED
     * - 无失败但存在不一致(0) -> PARTIAL
     * - 全部一致(1) -> SUCCESS
     * - 无结果 -> UNKNOWN
     */
    private String determineCompareStatusCode(CompareExecution execution) {
        try {
            // 业务口径：比对任务执行成功(包含有差异)即显示成功，只有执行失败才显示失败
            if (execution.getExecuteStatus() != null) {
                return getExecutionStatusCode(execution.getExecuteStatus());
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.warn("根据比对结果判定状态失败，降级使用执行表状态。executeId={}", execution.getExecuteId(), e);
            return getExecutionStatusCode(execution.getExecuteStatus());
        }
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) return "-";
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatDuration(LocalDateTime start, LocalDateTime end, Long durationMs) {
        // 优先使用 end-start 计算，兜底使用 durationMs；保证不出现负数
        if (start != null && end != null) {
            long ms = java.time.Duration.between(start, end).toMillis();
            if (ms < 0) ms = Math.abs(ms); // 防止历史数据异常
            return ms + "ms";
        }
        if (durationMs != null) {
            long ms = durationMs;
            if (ms < 0) ms = Math.abs(ms);
            return ms + "ms";
        }
        return "-";
    }
}
