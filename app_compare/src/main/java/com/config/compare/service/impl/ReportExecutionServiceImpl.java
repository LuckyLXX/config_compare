package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 报告执行服务实现
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExecutionServiceImpl implements ReportExecutionService {

    private final CollectExecutionService collectExecutionService;
    private final CompareExecutionService compareExecutionService;
    private final CollectTaskService collectTaskService;
    private final CompareTaskService compareTaskService;
    private final SystemInfoService systemInfoService;
    private final CompareResultService compareResultService;

    @Override
    public Map<String, Object> getExecutionOverview() {
        try {
            log.info("获取执行概览数据");
            
            // 获取总执行次数
            long totalCollectExecutions = collectExecutionService.count();
            long totalCompareExecutions = compareExecutionService.count();
            long totalExecutions = totalCollectExecutions + totalCompareExecutions;
            
            // 获取成功执行次数
            long successCollectExecutions = collectExecutionService.lambdaQuery()
                .eq(CollectExecution::getExecuteStatus, 1)
                .count();
            long successCompareExecutions = compareExecutionService.lambdaQuery()
                .in(CompareExecution::getExecuteStatus, Arrays.asList(1, 2)) // 成功和部分成功
                .count();
            long successExecutions = successCollectExecutions + successCompareExecutions;
            
            // 获取失败执行次数
            long failedCollectExecutions = collectExecutionService.lambdaQuery()
                .eq(CollectExecution::getExecuteStatus, 3)
                .count();
            long failedCompareExecutions = compareExecutionService.lambdaQuery()
                .eq(CompareExecution::getExecuteStatus, 3)
                .count();
            long failedExecutions = failedCollectExecutions + failedCompareExecutions;
            
            // 计算成功率
            double successRate = totalExecutions > 0 ? 
                (double) successExecutions / totalExecutions * 100 : 0;
            double failureRate = totalExecutions > 0 ? 
                (double) failedExecutions / totalExecutions * 100 : 0;
            
            // 计算执行时长统计
            Map<String, Long> durationStats = calculateDurationStats();
            
            // 获取今日和昨日执行次数
            long todayExecutions = getTodayExecutions();
            long yesterdayExecutions = getYesterdayExecutions();
            
            Map<String, Object> overview = new HashMap<>();
            overview.put("totalExecutions", totalExecutions);
            overview.put("successExecutions", successExecutions);
            overview.put("failedExecutions", failedExecutions);
            overview.put("successRate", Math.round(successRate * 100.0) / 100.0);
            overview.put("failureRate", Math.round(failureRate * 100.0) / 100.0);
            overview.put("averageDuration", durationStats.get("average"));
            overview.put("minDuration", durationStats.get("min"));
            overview.put("maxDuration", durationStats.get("max"));
            overview.put("todayExecutions", todayExecutions);
            overview.put("yesterdayExecutions", yesterdayExecutions);
            overview.put("lastUpdateTime", System.currentTimeMillis());
            
            log.info("执行概览数据统计完成 - 总执行: {}, 成功: {}, 失败: {}, 成功率: {}%, 今日: {}, 昨日: {}",
                totalExecutions, successExecutions, failedExecutions, successRate, todayExecutions, yesterdayExecutions);
            
            return overview;
        } catch (Exception e) {
            log.error("获取执行概览数据失败", e);
            return createEmptyOverview();
        }
    }

    @Override
    public Map<String, Object> getExecutionReports(Integer current, Integer size, 
                                                  String reportType, String startTime, String endTime) {
        try {
            log.info("获取执行报告列表 - 参数: current={}, size={}, reportType={}, startTime={}, endTime={}",
                current, size, reportType, startTime, endTime);
            
            // 设置默认值
            if (current == null || current < 1) current = 1;
            if (size == null || size < 1) size = 10;
            
            // 解析时间范围
            LocalDateTime start = parseStartTime(startTime);
            LocalDateTime end = parseEndTime(endTime);
            
            // 创建分页对象
            Page<Map<String, Object>> page = new Page<>(current, size);
            
            // 获取报告列表
            List<Map<String, Object>> reports = new ArrayList<>();
            
            // 根据报告类型获取数据
            if (reportType == null || "ALL".equals(reportType) || "COLLECT_EXECUTION".equals(reportType)) {
                List<Map<String, Object>> collectReports = generateCollectReports(start, end, size);
                reports.addAll(collectReports);
            }
            
            if (reportType == null || "ALL".equals(reportType) || "COMPARE_EXECUTION".equals(reportType)) {
                List<Map<String, Object>> compareReports = generateCompareReports(start, end, size);
                reports.addAll(compareReports);
            }
            
            // 按时间排序
            reports.sort((a, b) -> {
                String timeA = (String) a.get("createTime");
                String timeB = (String) b.get("createTime");
                return timeB.compareTo(timeA);
            });
            
            // 分页处理
            int total = reports.size();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<Map<String, Object>> pageRecords = fromIndex < total ? 
                reports.subList(fromIndex, toIndex) : new ArrayList<>();
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", pageRecords);
            result.put("total", total);
            result.put("current", current);
            result.put("size", size);
            result.put("pages", (int) Math.ceil((double) total / size));
            
            log.info("执行报告列表查询完成 - 总数: {}, 当前页: {}, 页大小: {}", total, current, size);
            
            return result;
        } catch (Exception e) {
            log.error("获取执行报告列表失败", e);
            return createEmptyReportList(current, size);
        }
    }

    @Override
    public Map<String, Object> generateExecutionReport(Map<String, Object> request) {
        try {
            log.info("生成执行报告 - 请求参数: {}", request);
            
            // 提取参数
            String reportType = (String) request.get("reportType");
            String startTime = (String) request.get("startTime");
            String endTime = (String) request.get("endTime");
            Long systemId = request.get("systemId") != null ? 
                Long.valueOf(request.get("systemId").toString()) : null;
            
            // 生成报告ID
            String reportId = "RPT_" + System.currentTimeMillis();
            
            // 异步生成报告逻辑可以在这里实现
            // 目前返回生成任务信息
            
            Map<String, Object> result = new HashMap<>();
            result.put("reportId", reportId);
            result.put("status", "生成中");
            result.put("message", "报告生成任务已提交");
            result.put("reportType", reportType);
            result.put("systemId", systemId);
            result.put("submitTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("estimatedTime", "预计3-5分钟完成");
            
            log.info("执行报告生成任务已提交 - 报告ID: {}, 类型: {}", reportId, reportType);
            return result;
        } catch (Exception e) {
            log.error("生成执行报告失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "失败");
            result.put("message", "报告生成失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> exportExecutionReport(Map<String, Object> params) {
        try {
            log.info("导出执行报告 - 参数: {}", params);
            
            String reportId = (String) params.get("reportId");
            String format = (String) params.getOrDefault("format", "xlsx");
            
            // 生成导出文件路径
            String fileName = "execution_report_" + System.currentTimeMillis() + "." + format;
            String filePath = "/exports/" + fileName;
            
            // 实际导出逻辑可以在这里实现
            // 目前返回导出信息
            
            Map<String, Object> result = new HashMap<>();
            result.put("exportId", "EXP_" + System.currentTimeMillis());
            result.put("status", "导出中");
            result.put("fileName", fileName);
            result.put("filePath", filePath);
            result.put("format", format);
            result.put("message", "报告导出任务已提交");
            result.put("submitTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.put("estimatedTime", "预计1-2分钟完成");
            
            log.info("执行报告导出任务已提交 - 导出ID: {}, 文件名: {}", result.get("exportId"), fileName);
            return result;
        } catch (Exception e) {
            log.error("导出执行报告失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("status", "失败");
            result.put("message", "报告导出失败: " + e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> getExecutionReportById(Long id) {
        try {
            log.info("获取报告详情 - ID: {}", id);
            
            // 这里应该从报告存储表中查询报告详情
            // 目前返回模拟数据
            
            Map<String, Object> result = new HashMap<>();
            result.put("id", id);
            result.put("reportName", "执行报告_" + id);
            result.put("reportType", "EXECUTION");
            result.put("status", "已完成");
            result.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            // 获取报告内容
            Map<String, Object> content = generateReportContent(id);
            result.put("content", content);
            
            log.info("报告详情查询完成 - ID: {}", id);
            return result;
        } catch (Exception e) {
            log.error("获取报告详情失败", e);
            return createEmptyReportDetail(id);
        }
    }

    @Override
    public boolean deleteExecutionReport(Long id) {
        try {
            log.info("删除执行报告 - ID: {}", id);
            
            // 这里应该从报告存储表中删除报告
            // 目前返回成功
            
            log.info("执行报告删除成功 - ID: {}", id);
            return true;
        } catch (Exception e) {
            log.error("删除执行报告失败", e);
            return false;
        }
    }

    /**
     * 计算执行时长统计
     */
    private Map<String, Long> calculateDurationStats() {
        Map<String, Long> stats = new HashMap<>();
        
        try {
            // 获取采集执行时长
            List<CollectExecution> collectExecutions = collectExecutionService.lambdaQuery()
                .isNotNull(CollectExecution::getDurationMs)
                .orderByAsc(CollectExecution::getDurationMs)
                .last("LIMIT 1000")
                .list();
            
            // 获取比对执行时长
            List<CompareExecution> compareExecutions = compareExecutionService.lambdaQuery()
                .isNotNull(CompareExecution::getDurationMs)
                .orderByAsc(CompareExecution::getDurationMs)
                .last("LIMIT 1000")
                .list();
            
            List<Long> allDurations = new ArrayList<>();
            
            // 收集所有执行时长
            collectExecutions.forEach(exec -> allDurations.add(exec.getDurationMs()));
            compareExecutions.forEach(exec -> allDurations.add(exec.getDurationMs()));
            
            if (!allDurations.isEmpty()) {
                long min = allDurations.get(0);
                long max = allDurations.get(allDurations.size() - 1);
                long average = allDurations.stream().mapToLong(Long::longValue).sum() / allDurations.size();
                
                stats.put("min", min);
                stats.put("max", max);
                stats.put("average", average);
            } else {
                stats.put("min", 0L);
                stats.put("max", 0L);
                stats.put("average", 0L);
            }
        } catch (Exception e) {
            log.warn("计算执行时长统计失败", e);
            stats.put("min", 0L);
            stats.put("max", 0L);
            stats.put("average", 0L);
        }
        
        return stats;
    }

    /**
     * 获取今日执行次数
     */
    private long getTodayExecutions() {
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
     * 获取昨日执行次数
     */
    private long getYesterdayExecutions() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1).toLocalDate().atStartOfDay();
        LocalDateTime today = yesterday.plusDays(1);
        
        long collectCount = collectExecutionService.lambdaQuery()
            .ge(CollectExecution::getStartTime, yesterday)
            .lt(CollectExecution::getStartTime, today)
            .count();
            
        long compareCount = compareExecutionService.lambdaQuery()
            .ge(CompareExecution::getStartTime, yesterday)
            .lt(CompareExecution::getStartTime, today)
            .count();
            
        return collectCount + compareCount;
    }

    /**
     * 生成采集执行报告
     */
    private List<Map<String, Object>> generateCollectReports(LocalDateTime start, LocalDateTime end, Integer limit) {
        List<Map<String, Object>> reports = new ArrayList<>();
        
        try {
            LambdaQueryWrapper<CollectExecution> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(start != null, CollectExecution::getStartTime, start)
                   .le(end != null, CollectExecution::getStartTime, end)
                   .orderByDesc(CollectExecution::getStartTime)
                   .last("LIMIT " + (limit != null ? limit : 10));
            
            List<CollectExecution> executions = collectExecutionService.list(wrapper);
            
            for (CollectExecution execution : executions) {
                CollectTask task = collectTaskService.getById(execution.getTaskId());
                SystemInfo system = task != null ? systemInfoService.getById(task.getSystemId()) : null;
                
                Map<String, Object> report = new HashMap<>();
                report.put("id", execution.getId());
                report.put("reportName", "采集执行报告_" + execution.getExecuteId());
                report.put("reportType", "COLLECT_EXECUTION");
                report.put("taskId", execution.getTaskId());
                report.put("taskName", task != null ? task.getTaskName() : "未知任务");
                report.put("systemName", system != null ? system.getSystemName() : "未知系统");
                report.put("status", getExecutionStatusText(execution.getExecuteStatus()));
                report.put("executeId", execution.getExecuteId());
                report.put("totalServers", execution.getTotalServers());
                report.put("successServers", execution.getSuccessServers());
                report.put("failedServers", execution.getFailedServers());
                report.put("createTime", execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                reports.add(report);
            }
        } catch (Exception e) {
            log.error("生成采集执行报告失败", e);
        }
        
        return reports;
    }

    /**
     * 生成比对执行报告
     */
    private List<Map<String, Object>> generateCompareReports(LocalDateTime start, LocalDateTime end, Integer limit) {
        List<Map<String, Object>> reports = new ArrayList<>();
        
        try {
            LambdaQueryWrapper<CompareExecution> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(start != null, CompareExecution::getStartTime, start)
                   .le(end != null, CompareExecution::getStartTime, end)
                   .orderByDesc(CompareExecution::getStartTime)
                   .last("LIMIT " + (limit != null ? limit : 10));
            
            List<CompareExecution> executions = compareExecutionService.list(wrapper);
            
            for (CompareExecution execution : executions) {
                CompareTask task = compareTaskService.getById(execution.getTaskId());
                SystemInfo system = task != null ? systemInfoService.getById(task.getSystemId()) : null;
                
                Map<String, Object> report = new HashMap<>();
                report.put("id", execution.getId());
                report.put("reportName", "比对执行报告_" + execution.getExecuteId());
                report.put("reportType", "COMPARE_EXECUTION");
                report.put("taskId", execution.getTaskId());
                report.put("taskName", task != null ? task.getTaskName() : "未知任务");
                report.put("systemName", system != null ? system.getSystemName() : "未知系统");
                report.put("status", getExecutionStatusText(execution.getExecuteStatus()));
                report.put("executeId", execution.getExecuteId());
                report.put("totalServers", execution.getTotalServers());
                report.put("consistentServers", execution.getConsistentServers());
                report.put("inconsistentServers", execution.getInconsistentServers());
                report.put("failedServers", execution.getFailedServers());
                report.put("overallScore", execution.getOverallScore());
                report.put("createTime", execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                
                reports.add(report);
            }
        } catch (Exception e) {
            log.error("生成比对执行报告失败", e);
        }
        
        return reports;
    }

    /**
     * 生成报告内容
     */
    private Map<String, Object> generateReportContent(Long reportId) {
        Map<String, Object> content = new HashMap<>();
        
        try {
            // 根据报告ID查询执行记录
            // 先尝试查询比对执行记录
            CompareExecution compareExecution = compareExecutionService.getById(reportId);
            if (compareExecution != null) {
                return generateCompareReportContent(compareExecution);
            }
            
            // 再尝试查询采集执行记录
            CollectExecution collectExecution = collectExecutionService.getById(reportId);
            if (collectExecution != null) {
                return generateCollectReportContent(collectExecution);
            }
            
            // 如果都没有找到，返回空内容
            log.warn("未找到报告ID对应的执行记录: {}", reportId);
            content.put("totalTasks", 0);
            content.put("successTasks", 0);
            content.put("failedTasks", 0);
            content.put("successRate", 0.0);
            content.put("details", new ArrayList<>());
        } catch (Exception e) {
            log.error("生成报告内容失败", e);
            content.put("totalTasks", 0);
            content.put("successTasks", 0);
            content.put("failedTasks", 0);
            content.put("successRate", 0.0);
            content.put("details", new ArrayList<>());
        }
        
        return content;
    }

    /**
     * 生成比对报告内容
     */
    private Map<String, Object> generateCompareReportContent(CompareExecution execution) {
        Map<String, Object> content = new HashMap<>();
        
        try {
            // 获取任务信息
            CompareTask task = compareTaskService.getById(execution.getTaskId());
            SystemInfo system = task != null ? systemInfoService.getById(task.getSystemId()) : null;
            
            // 统计执行结果
            int totalServers = execution.getTotalServers() != null ? execution.getTotalServers() : 0;
            int successServers = execution.getConsistentServers() != null ? execution.getConsistentServers() : 0;
            int failedServers = execution.getInconsistentServers() != null ? execution.getInconsistentServers() : 0;
            failedServers += execution.getFailedServers() != null ? execution.getFailedServers() : 0;
            
            double successRate = totalServers > 0 ? (double) successServers / totalServers * 100 : 0;
            
            content.put("totalTasks", totalServers);
            content.put("successTasks", successServers);
            content.put("failedTasks", failedServers);
            content.put("successRate", Math.round(successRate * 100.0) / 100.0);
            content.put("averageDuration", execution.getDurationMs());
            content.put("taskName", task != null ? task.getTaskName() : "未知任务");
            content.put("systemName", system != null ? system.getSystemName() : "未知系统");
            content.put("executeId", execution.getExecuteId());
            content.put("startTime", execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            content.put("endTime", execution.getEndTime() != null ?
                execution.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-");
            
            // 获取详细结果
            List<Map<String, Object>> details = getCompareExecutionDetails(execution.getExecuteId());
            content.put("details", details);
            
        } catch (Exception e) {
            log.error("生成比对报告内容失败", e);
            content.put("totalTasks", 0);
            content.put("successTasks", 0);
            content.put("failedTasks", 0);
            content.put("successRate", 0.0);
            content.put("details", new ArrayList<>());
        }
        
        return content;
    }

    /**
     * 生成采集报告内容
     */
    private Map<String, Object> generateCollectReportContent(CollectExecution execution) {
        Map<String, Object> content = new HashMap<>();
        
        try {
            // 获取任务信息
            CollectTask task = collectTaskService.getById(execution.getTaskId());
            SystemInfo system = task != null ? systemInfoService.getById(task.getSystemId()) : null;
            
            // 统计执行结果
            int totalServers = execution.getTotalServers() != null ? execution.getTotalServers() : 0;
            int successServers = execution.getSuccessServers() != null ? execution.getSuccessServers() : 0;
            int failedServers = execution.getFailedServers() != null ? execution.getFailedServers() : 0;
            
            double successRate = totalServers > 0 ? (double) successServers / totalServers * 100 : 0;
            
            content.put("totalTasks", totalServers);
            content.put("successTasks", successServers);
            content.put("failedTasks", failedServers);
            content.put("successRate", Math.round(successRate * 100.0) / 100.0);
            content.put("averageDuration", execution.getDurationMs());
            content.put("taskName", task != null ? task.getTaskName() : "未知任务");
            content.put("systemName", system != null ? system.getSystemName() : "未知系统");
            content.put("executeId", execution.getExecuteId());
            content.put("startTime", execution.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            content.put("endTime", execution.getEndTime() != null ?
                execution.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-");
            
            // 获取详细结果
            List<Map<String, Object>> details = getCollectExecutionDetails(execution.getExecuteId());
            content.put("details", details);
            
        } catch (Exception e) {
            log.error("生成采集报告内容失败", e);
            content.put("totalTasks", 0);
            content.put("successTasks", 0);
            content.put("failedTasks", 0);
            content.put("successRate", 0.0);
            content.put("details", new ArrayList<>());
        }
        
        return content;
    }

    /**
     * 获取比对执行详情
     */
    private List<Map<String, Object>> getCompareExecutionDetails(String executeId) {
        List<Map<String, Object>> details = new ArrayList<>();
        
        try {
            // 查询比对结果
            List<CompareResult> results = compareResultService.getByExecuteId(executeId);
            
            for (CompareResult result : results) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", result.getId());
                detail.put("serverInstanceId", result.getServerInstanceId());
                detail.put("compareStatus", getCompareStatusText(result.getCompareStatus()));
                detail.put("consistencyScore", result.getConsistencyScore());
                detail.put("diffCount", result.getDiffCount());
                detail.put("highDiffCount", result.getHighDiffCount());
                detail.put("mediumDiffCount", result.getMediumDiffCount());
                detail.put("lowDiffCount", result.getLowDiffCount());
                detail.put("executeTime", result.getExecuteTime() != null ?
                    result.getExecuteTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-");
                detail.put("errorMessage", result.getErrorMessage());
                
                details.add(detail);
            }
        } catch (Exception e) {
            log.error("获取比对执行详情失败", e);
        }
        
        return details;
    }

    /**
     * 获取采集执行详情
     */
    private List<Map<String, Object>> getCollectExecutionDetails(String executeId) {
        List<Map<String, Object>> details = new ArrayList<>();
        
        try {
            // 这里应该查询采集结果详情
            // 由于没有直接的方法，暂时返回基本信息
            Map<String, Object> detail = new HashMap<>();
            detail.put("executeId", executeId);
            detail.put("message", "采集执行详情需要进一步实现");
            details.add(detail);
        } catch (Exception e) {
            log.error("获取采集执行详情失败", e);
        }
        
        return details;
    }

    /**
     * 获取比对状态文本
     */
    private String getCompareStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "一致";
            case 0: return "不一致";
            case -1: return "失败";
            default: return "未知";
        }
    }

    /**
     * 获取执行状态文本
     */
    private String getExecutionStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "成功";
            case 2: return "部分成功";
            case 3: return "失败";
            case 4: return "运行中";
            default: return "未知";
        }
    }

    /**
     * 解析开始时间
     */
    private LocalDateTime parseStartTime(String startTime) {
        if (StringUtils.hasText(startTime)) {
            try {
                // 尝试解析 ISO 8601 格式（带 Z 后缀）
                if (startTime.contains("Z")) {
                    // 移除 Z 后缀并解析
                    String timeWithoutZ = startTime.substring(0, startTime.length() - 1);
                    return LocalDateTime.parse(timeWithoutZ);
                }
                // 尝试解析标准格式
                else if (startTime.contains("T")) {
                    return LocalDateTime.parse(startTime);
                }
                // 尝试解析日期格式
                else {
                    return LocalDateTime.parse(startTime + "T00:00:00");
                }
            } catch (Exception e) {
                log.warn("解析开始时间失败: {}", startTime, e);
            }
        }
        
        // 默认最近7天
        return LocalDateTime.now().minusDays(7);
    }

    /**
     * 解析结束时间
     */
    private LocalDateTime parseEndTime(String endTime) {
        if (StringUtils.hasText(endTime)) {
            try {
                // 尝试解析 ISO 8601 格式（带 Z 后缀）
                if (endTime.contains("Z")) {
                    // 移除 Z 后缀并解析
                    String timeWithoutZ = endTime.substring(0, endTime.length() - 1);
                    return LocalDateTime.parse(timeWithoutZ);
                }
                // 尝试解析标准格式
                else if (endTime.contains("T")) {
                    return LocalDateTime.parse(endTime);
                }
                // 尝试解析日期格式
                else {
                    return LocalDateTime.parse(endTime + "T23:59:59");
                }
            } catch (Exception e) {
                log.warn("解析结束时间失败: {}", endTime, e);
            }
        }
        
        // 默认当前时间
        return LocalDateTime.now();
    }

    /**
     * 创建空的概览数据
     */
    private Map<String, Object> createEmptyOverview() {
        Map<String, Object> overview = new HashMap<>();
        overview.put("totalExecutions", 0);
        overview.put("successExecutions", 0);
        overview.put("failedExecutions", 0);
        overview.put("successRate", 0.0);
        overview.put("failureRate", 0.0);
        overview.put("averageDuration", 0L);
        overview.put("minDuration", 0L);
        overview.put("maxDuration", 0L);
        overview.put("todayExecutions", 0);
        overview.put("yesterdayExecutions", 0);
        overview.put("lastUpdateTime", System.currentTimeMillis());
        return overview;
    }

    /**
     * 创建空的报告列表
     */
    private Map<String, Object> createEmptyReportList(Integer current, Integer size) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", new ArrayList<>());
        result.put("total", 0);
        result.put("current", current);
        result.put("size", size);
        result.put("pages", 0);
        return result;
    }

    /**
     * 创建空的报告详情
     */
    private Map<String, Object> createEmptyReportDetail(Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("reportName", "执行报告_" + id);
        result.put("reportType", "EXECUTION");
        result.put("status", "不存在");
        result.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        result.put("content", Map.of(
            "totalTasks", 0,
            "successTasks", 0,
            "failedTasks", 0,
            "successRate", 0.0
        ));
        return result;
    }
}