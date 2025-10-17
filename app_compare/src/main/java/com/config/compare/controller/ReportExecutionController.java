package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.ReportExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 执行报告Controller
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/report/executions")
@RequiredArgsConstructor
@Tag(name = "执行报告管理")
@Validated
public class ReportExecutionController {

    private final ReportExecutionService reportExecutionService;

    @Operation(summary = "获取执行概览数据")
    @GetMapping("/overview")
    public Result<Map<String, Object>> getExecutionOverview() {
        try {
            log.info("获取执行概览数据 - 开始处理请求");
            
            Map<String, Object> overview = reportExecutionService.getExecutionOverview();
            
            log.info("获取执行概览数据 - 返回结果: {}", overview);
            return Result.success("查询成功", overview);
        } catch (Exception e) {
            log.error("获取执行概览数据失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取执行报告列表")
    @GetMapping
    public Result<Object> getExecutionReports(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "报告类型") @RequestParam(required = false) String reportType,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        try {
            log.info("获取执行报告列表 - 参数: current={}, size={}, reportType={}, startTime={}, endTime={}",
                current, size, reportType, startTime, endTime);
            
            Map<String, Object> result = reportExecutionService.getExecutionReports(
                current, size, reportType, startTime, endTime);
            
            log.info("获取执行报告列表 - 返回结果: {}", result);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取执行报告列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "生成执行报告")
    @PostMapping("/generate")
    public Result<Map<String, Object>> generateExecutionReport(@RequestBody Map<String, Object> request) {
        try {
            log.info("生成执行报告 - 请求参数: {}", request);
            
            Map<String, Object> result = reportExecutionService.generateExecutionReport(request);
            
            log.info("生成执行报告 - 返回结果: {}", result);
            return Result.success("生成成功", result);
        } catch (Exception e) {
            log.error("生成执行报告失败", e);
            return Result.error("生成失败：" + e.getMessage());
        }
    }

    @Operation(summary = "导出执行报告")
    @PostMapping("/export")
    public void exportExecutionReport(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            log.info("导出执行报告 - 参数: {}", params);
            
            Map<String, Object> result = reportExecutionService.exportExecutionReport(params);
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = (String) result.get("fileName");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            
            log.info("导出执行报告 - 返回结果: {}", result);
            
            // 实际实现时需要根据result中的文件路径写入Excel数据到response.getOutputStream()
            // 目前返回空响应，前端需要根据返回的导出状态进行后续处理
            
        } catch (Exception e) {
            log.error("导出执行报告失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取报告详情")
    @GetMapping("/{id}")
    public Result<Object> getExecutionReportById(@Parameter(description = "报告ID") @PathVariable Long id) {
        try {
            log.info("获取报告详情 - ID: {}", id);
            
            Map<String, Object> result = reportExecutionService.getExecutionReportById(id);
            
            log.info("获取报告详情 - 返回结果: {}", result);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取报告详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除执行报告")
    @DeleteMapping("/{id}")
    public Result<Void> deleteExecutionReport(@Parameter(description = "报告ID") @PathVariable Long id) {
        try {
            log.info("删除执行报告 - ID: {}", id);
            
            boolean success = reportExecutionService.deleteExecutionReport(id);
            
            if (success) {
                log.info("删除执行报告成功 - ID: {}", id);
                return Result.success("删除成功");
            } else {
                log.warn("删除执行报告失败 - ID: {}", id);
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除执行报告失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }
}
