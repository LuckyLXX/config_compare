package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.ReportDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * 报告仪表板Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/report/dashboard")
@RequiredArgsConstructor
@Tag(name = "报告仪表板")
@Validated
public class ReportDashboardController {

    private final ReportDashboardService reportDashboardService;

    @Operation(summary = "获取仪表板概览数据")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getDashboardOverview(
            @Parameter(description = "时间范围") @RequestParam(required = false) String timeRange,
            @Parameter(description = "开始时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String endDate) {
        try {
            if (startDate != null || endDate != null) {
                LocalDateTime start = parseFlexibleDateTime(startDate);
                LocalDateTime end = parseFlexibleDateTime(endDate);
                Map<String, Object> result = reportDashboardService.getDashboardOverview(start, end);
                return Result.success("查询成功", result);
            }
            Map<String, Object> result = reportDashboardService.getDashboardOverview(timeRange);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取仪表板概览数据失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取系统状态统计")
    @GetMapping("/system-stats")
    public Result<Map<String, Object>> getSystemStats(
            @Parameter(description = "系统ID") @RequestParam(required = false) Long systemId) {
        try {
            Map<String, Object> result = reportDashboardService.getSystemStats(systemId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取系统状态统计失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取任务执行趋势")
    @GetMapping("/task-trends")
    public Result<Map<String, Object>> getTaskTrends(
            @Parameter(description = "时间范围") @RequestParam(required = false) String timeRange,
            @Parameter(description = "开始时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String endDate,
            @Parameter(description = "周期: 7d/30d/90d") @RequestParam(required = false) String period) {
        try {
            if (startDate != null || endDate != null) {
                LocalDateTime start = parseFlexibleDateTime(startDate);
                LocalDateTime end = parseFlexibleDateTime(endDate);
                Map<String, Object> result = reportDashboardService.getTaskTrends(start, end, period);
                return Result.success("查询成功", result);
            }
            Map<String, Object> result = reportDashboardService.getTaskTrends(timeRange);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取任务执行趋势失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取比对结果分布")
    @GetMapping("/compare-distribution")
    public Result<Map<String, Object>> getCompareDistribution(
            @Parameter(description = "时间范围") @RequestParam(required = false) String timeRange,
            @Parameter(description = "开始时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String endDate) {
        try {
            if (startDate != null || endDate != null) {
                LocalDateTime start = parseFlexibleDateTime(startDate);
                LocalDateTime end = parseFlexibleDateTime(endDate);
                Map<String, Object> result = reportDashboardService.getCompareDistribution(start, end);
                return Result.success("查询成功", result);
            }
            Map<String, Object> result = reportDashboardService.getCompareDistribution(timeRange);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取比对结果分布失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取最近执行记录")
    @GetMapping("/recent-executions")
    public Result<Map<String, Object>> getRecentExecutions(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "开始时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束时间(yyyy-MM-dd HH:mm:ss)") @RequestParam(required = false) String endDate) {
        try {
            if (startDate != null || endDate != null) {
                LocalDateTime start = parseFlexibleDateTime(startDate);
                LocalDateTime end = parseFlexibleDateTime(endDate);
                Map<String, Object> result = reportDashboardService.getRecentExecutions(start, end, limit);
                return Result.success("查询成功", result);
            }
            Map<String, Object> result = reportDashboardService.getRecentExecutions(limit);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取最近执行记录失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取告警信息")
    @GetMapping("/alerts")
    public Result<Map<String, Object>> getAlerts(
            @Parameter(description = "告警级别") @RequestParam(required = false) String level) {
        try {
            Map<String, Object> result = reportDashboardService.getAlerts(level);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取告警信息失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 兼容解析多种日期时间格式：
     * - yyyy-MM-dd HH:mm:ss
     * - ISO_LOCAL_DATE_TIME (yyyy-MM-dd'T'HH:mm[:ss][.SSS])
     * - ISO_OFFSET_DATE_TIME (带时区偏移)
     * - ISO_INSTANT (Z时区)
     */
    private LocalDateTime parseFlexibleDateTime(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return LocalDateTime.parse(text, f);
        } catch (DateTimeParseException ignore) { }
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) { }
        try {
            return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
        } catch (DateTimeParseException ignore) { }
        try {
            return Instant.parse(text).atZone(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            log.warn("无法解析时间参数: {}", text);
            return null;
        }
    }
}
