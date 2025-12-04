package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.config.compare.entity.CompareTask;
import com.config.compare.entity.CompareExecution;
import com.config.compare.entity.CompareResult;
import com.config.compare.entity.CompareRule;
import com.config.compare.entity.ServerInstance;
import com.config.compare.service.CompareTaskService;
import com.config.compare.service.CompareResultService;
import com.config.compare.service.ServerInstanceService;
import com.config.compare.service.CompareDiffDetailService;
import com.config.compare.entity.CompareDiffDetail;
import com.config.compare.service.ConfigBaselineService;
import com.config.compare.entity.ConfigBaseline;
import com.config.compare.service.CollectResultEntityService;
import com.config.compare.entity.CollectResultEntity;
import com.config.compare.service.SystemInfoService;
import com.config.compare.entity.SystemInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.config.compare.compare.model.CompareContext;
import com.config.compare.compare.model.CompareResultModel;
import com.config.compare.compare.model.AlignedLine;
import com.config.compare.compare.manager.CompareAlgorithmManager;

/**
 * 比对管理Controller（简化版本）
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/compare")
@RequiredArgsConstructor
@Tag(name = "比对管理")
@Validated
public class CompareController {

    private final CompareTaskService compareTaskService;
    private final CompareResultService compareResultService;
    private final ServerInstanceService serverInstanceService;
    private final CompareDiffDetailService compareDiffDetailService;
    private final ConfigBaselineService configBaselineService;
    private final CollectResultEntityService collectResultEntityService;
    private final SystemInfoService systemInfoService;
    private final CompareAlgorithmManager compareAlgorithmManager; // 【新增】用于生成对齐行数据

    // ==================== 比对任务相关接口 ====================
    
    @Operation(summary = "获取比对任务列表")
    @GetMapping("/tasks")
    public Result<Object> getTaskList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "系统ID") @RequestParam(required = false) Long systemId,
            @Parameter(description = "执行类型") @RequestParam(required = false) Integer executeType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        try {
            IPage<CompareTask> page = compareTaskService.pageQuery(current, size, taskName, systemId, executeType, status);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("获取比对任务列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建比对任务")
    @PostMapping("/tasks")
    public Result<Void> createTask(@RequestBody CompareTask task) {
        try {
            boolean success = compareTaskService.createTask(task);
            return success ? Result.success("创建成功") : Result.error("创建失败");
        } catch (Exception e) {
            log.error("创建比对任务失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新比对任务")
    @PutMapping("/tasks/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @RequestBody CompareTask task) {
        try {
            task.setId(id);
            boolean success = compareTaskService.updateTask(task);
            return success ? Result.success("更新成功") : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新比对任务失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除比对任务")
    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        try {
            boolean success = compareTaskService.deleteTask(id);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除比对任务失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取比对任务详情")
    @GetMapping("/tasks/{id}")
    public Result<Object> getTaskById(@PathVariable Long id) {
        try {
            CompareTask task = compareTaskService.getById(id);
            return task != null ? Result.success("查询成功", task) : Result.error("任务不存在");
        } catch (Exception e) {
            log.error("获取比对任务详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用比对任务")
    @PutMapping("/tasks/{id}/status")
    public Result<Void> toggleTaskStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer status = request.get("status");
            if (status == null) {
                return Result.error("状态参数不能为空");
            }
            boolean success = compareTaskService.toggleTaskStatus(id, status);
            return success ? Result.success("状态更新成功") : Result.error("状态更新失败");
        } catch (Exception e) {
            log.error("更新比对任务状态失败", e);
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "立即执行比对任务")
    @PostMapping("/tasks/{id}/execute")
    public Result<String> executeTask(@PathVariable Long id) {
        try {
            String executeId = compareTaskService.executeTask(id);
            return Result.success("任务执行成功", executeId);
        } catch (Exception e) {
            log.error("执行比对任务失败", e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }

    @Operation(summary = "批量执行比对任务")
    @PostMapping("/tasks/batch-execute")
    public Result<Void> batchExecute(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> taskIds = request.get("taskIds");
            if (taskIds == null || taskIds.isEmpty()) {
                return Result.error("任务ID列表不能为空");
            }
            boolean success = compareTaskService.batchExecute(taskIds.toArray(new Long[0]));
            return success ? Result.success("批量执行成功") : Result.error("批量执行失败");
        } catch (Exception e) {
            log.error("批量执行比对任务失败", e);
            return Result.error("批量执行失败：" + e.getMessage());
        }
    }

    @Operation(summary = "暂停比对任务")
    @PostMapping("/tasks/{id}/pause")
    public Result<Void> pauseTask(@PathVariable Long id) {
        try {
            boolean success = compareTaskService.toggleTaskStatus(id, 0); // 0表示暂停
            return success ? Result.success("暂停成功") : Result.error("暂停失败");
        } catch (Exception e) {
            log.error("暂停比对任务失败", e);
            return Result.error("暂停失败：" + e.getMessage());
        }
    }

    @Operation(summary = "恢复比对任务")
    @PostMapping("/tasks/{id}/resume")
    public Result<Void> resumeTask(@PathVariable Long id) {
        try {
            boolean success = compareTaskService.toggleTaskStatus(id, 1); // 1表示启用
            return success ? Result.success("恢复成功") : Result.error("恢复失败");
        } catch (Exception e) {
            log.error("恢复比对任务失败", e);
            return Result.error("恢复失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取任务执行历史")
    @GetMapping("/tasks/{taskId}/executions")
    public Result<Object> getTaskExecutions(@PathVariable Long taskId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // TODO: 实现获取任务执行历史，需要CompareExecutionService
            return Result.success("查询成功", Map.of("records", List.of(), "total", 0));
        } catch (Exception e) {
            log.error("获取任务执行历史失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 比对结果相关接口 ====================

    @Operation(summary = "获取比对结果列表")
    @GetMapping("/results")
    public Result<Object> getResultList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String taskName,
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) Integer compareStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            IPage<CompareResult> page = new Page<>(current, size);
            
            LambdaQueryWrapper<CompareResult> queryWrapper = new LambdaQueryWrapper<>();
            
            // 添加查询条件
            if (taskId != null) {
                queryWrapper.eq(CompareResult::getTaskId, taskId);
            }
            
            if (compareStatus != null) {
                queryWrapper.eq(CompareResult::getCompareStatus, compareStatus);
            }
            
            // 按任务名称搜索（需要通过关联查询）
            if (taskName != null && !taskName.trim().isEmpty()) {
                // 先查询匹配任务名称的任务ID列表
                LambdaQueryWrapper<CompareTask> taskQueryWrapper = new LambdaQueryWrapper<>();
                taskQueryWrapper.like(CompareTask::getTaskName, taskName.trim());
                List<CompareTask> matchingTasks = compareTaskService.list(taskQueryWrapper);
                
                if (matchingTasks.isEmpty()) {
                    // 如果没有匹配的任务，返回空结果
                    return Result.success("查询成功", Map.of("records", List.of(), "total", 0, "current", current, "size", size));
                } else {
                    // 获取匹配任务的ID列表
                    List<Long> taskIds = matchingTasks.stream().map(CompareTask::getId).collect(Collectors.toList());
                    queryWrapper.in(CompareResult::getTaskId, taskIds);
                }
            }
            
            // 按系统ID搜索（需要通过关联查询）
            if (systemId != null) {
                // 先查询匹配系统ID的任务ID列表
                LambdaQueryWrapper<CompareTask> taskQueryWrapper = new LambdaQueryWrapper<>();
                taskQueryWrapper.eq(CompareTask::getSystemId, systemId);
                List<CompareTask> matchingTasks = compareTaskService.list(taskQueryWrapper);
                
                if (matchingTasks.isEmpty()) {
                    // 如果没有匹配的任务，返回空结果
                    return Result.success("查询成功", Map.of("records", List.of(), "total", 0, "current", current, "size", size));
                } else {
                    // 获取匹配任务的ID列表
                    List<Long> taskIds = matchingTasks.stream().map(CompareTask::getId).collect(Collectors.toList());
                    queryWrapper.in(CompareResult::getTaskId, taskIds);
                }
            }
            
            if (startTime != null && !startTime.trim().isEmpty()) {
                queryWrapper.ge(CompareResult::getExecuteTime, LocalDateTime.parse(startTime));
            }
            
            if (endTime != null && !endTime.trim().isEmpty()) {
                queryWrapper.le(CompareResult::getExecuteTime, LocalDateTime.parse(endTime));
            }
            
            // 按执行时间倒序排列
            queryWrapper.orderByDesc(CompareResult::getExecuteTime);
            
            IPage<CompareResult> resultPage = compareResultService.page(page, queryWrapper);
            
            // 转换为前端需要的格式
            List<Map<String, Object>> records = resultPage.getRecords().stream()
                .map(result -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", result.getId());
                    record.put("taskId", result.getTaskId());
                    record.put("executeId", result.getExecuteId());
                    record.put("baselineId", result.getBaselineId());
                    record.put("serverInstanceId", result.getServerInstanceId());
                    record.put("collectResultId", result.getCollectResultId());
                    record.put("compareStatus", result.getCompareStatus());
                    record.put("consistencyScore", result.getConsistencyScore());
                    record.put("highDiffCount", result.getHighDiffCount());
                    record.put("mediumDiffCount", result.getMediumDiffCount());
                    record.put("lowDiffCount", result.getLowDiffCount());
                    record.put("diffSummary", result.getDiffSummary());
                    record.put("executeTime", result.getExecuteTime());
                    record.put("durationMs", result.getDurationMs());
                    record.put("createTime", result.getCreateTime());
                    record.put("updateTime", result.getUpdateTime());
                    record.put("errorMessage", result.getErrorMessage());
                    
                    // 查询差异类型统计
                    try {
                        LambdaQueryWrapper<CompareDiffDetail> diffWrapper = new LambdaQueryWrapper<>();
                        diffWrapper.eq(CompareDiffDetail::getResultId, result.getId());
                        List<CompareDiffDetail> diffDetails = compareDiffDetailService.list(diffWrapper);
                        
                        long addCount = diffDetails.stream().filter(d -> "ADD".equals(d.getDiffType())).count();
                        long deleteCount = diffDetails.stream().filter(d -> "DELETE".equals(d.getDiffType())).count();
                        long modifyCount = diffDetails.stream().filter(d -> "MODIFY".equals(d.getDiffType())).count();
                        
                        record.put("addCount", addCount);
                        record.put("deleteCount", deleteCount);
                        record.put("modifyCount", modifyCount);
                        record.put("diffCount", addCount + deleteCount + modifyCount);
                    } catch (Exception e) {
                        log.warn("获取差异类型统计失败: resultId={}", result.getId(), e);
                        record.put("addCount", 0);
                        record.put("deleteCount", 0);
                        record.put("modifyCount", 0);
                        record.put("diffCount", 0);
                    }
                    
                    // 获取关联的任务信息
                    try {
                        CompareTask task = compareTaskService.getById(result.getTaskId());
                        if (task != null) {
                            record.put("taskName", task.getTaskName());
                            record.put("systemId", task.getSystemId());
                        }
                    } catch (Exception e) {
                        log.warn("获取任务信息失败: taskId={}", result.getTaskId(), e);
                        record.put("taskName", "未知任务");
                    }
                    
                    // 获取关联的基线信息（用于获取serverTypeId、categoryId和baselineName）
                    try {
                        ConfigBaseline baseline = configBaselineService.getById(result.getBaselineId());
                        if (baseline != null) {
                            record.put("serverTypeId", baseline.getServerTypeId());
                            record.put("categoryId", baseline.getCategoryId());
                            record.put("categoryName", baseline.getFileName() != null ? 
                                baseline.getFileName().replace(".txt", "") : "未知配置");
                            record.put("baselineName", baseline.getBaselineName());  // 【新增】基线名称
                        }
                    } catch (Exception e) {
                        log.warn("获取基线信息失败: baselineId={}", result.getBaselineId(), e);
                    }
                    
                    // 获取关联的服务器信息
                    try {
                        ServerInstance server = serverInstanceService.getById(result.getServerInstanceId());
                        if (server != null) {
                            String systemName = "未知系统";
                            if (server.getSystemId() != null) {
                                SystemInfo systemInfo = systemInfoService.getById(server.getSystemId());
                                if (systemInfo != null) {
                                    systemName = systemInfo.getSystemName();
                                }
                            }
                            record.put("systemName", systemName);
                            record.put("serverInstance", Map.of(
                                "hostname", server.getInstanceName(),
                                "serverIp", server.getServerIp()
                            ));
                        }
                    } catch (Exception e) {
                        log.warn("获取服务器信息失败: serverId={}", result.getServerInstanceId(), e);
                        record.put("systemName", "未知系统");
                        record.put("serverInstance", Map.of("hostname", "未知服务器"));
                    }
                    
                    return record;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", resultPage.getTotal());
            result.put("current", resultPage.getCurrent());
            result.put("size", resultPage.getSize());
            
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取比对结果列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取比对结果详情")
    @GetMapping("/results/{id}")
    public Result<Object> getResultById(@PathVariable Long id) {
        try {
            // TODO: 实现获取比对结果详情，需要CompareResultService
            return Result.success("查询成功", Map.of("id", id, "status", "一致"));
        } catch (Exception e) {
            log.error("获取比对结果详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据任务ID获取比对结果")
    @GetMapping("/tasks/{taskId}/results")
    public Result<Object> getResultsByTaskId(@PathVariable Long taskId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // TODO: 实现根据任务ID获取比对结果，需要CompareResultService
            return Result.success("查询成功", Map.of("records", List.of(), "total", 0));
        } catch (Exception e) {
            log.error("根据任务ID获取比对结果失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据执行ID获取比对结果")
    @GetMapping("/executions/{executeId}/results")
    public Result<Object> getResultsByExecuteId(@PathVariable String executeId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // TODO: 实现根据执行ID获取比对结果，需要CompareResultService
            return Result.success("查询成功", Map.of("records", List.of(), "total", 0));
        } catch (Exception e) {
            log.error("根据执行ID获取比对结果失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取差异详情")
    @GetMapping("/results/{resultId}/diffs")
    public Result<Object> getDiffDetails(@PathVariable Long resultId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // 验证比对结果是否存在
            CompareResult compareResult = compareResultService.getById(resultId);
            if (compareResult == null) {
                return Result.error("比对结果不存在");
            }
            
            // 分页查询差异详情
            Page<CompareDiffDetail> page = new Page<>(current, size);
            IPage<CompareDiffDetail> diffPage = compareDiffDetailService.getDiffDetailsByResultId(resultId, page);
            
            // 获取基线内容
            String baselineContent = "";
            try {
                ConfigBaseline baseline = configBaselineService.getById(compareResult.getBaselineId());
                if (baseline != null) {
                    baselineContent = baseline.getConfigContent();
                    log.info("🔍 获取基线内容成功: baselineId={}, 内容长度={}",
                            compareResult.getBaselineId(),
                            baselineContent != null ? baselineContent.length() : 0);
                    log.debug("🔍 基线内容前500字符: {}",
                            baselineContent != null && baselineContent.length() > 500 ?
                            baselineContent.substring(0, 500) + "..." : baselineContent);
                } else {
                    log.warn("🔍 基线配置不存在: baselineId={}", compareResult.getBaselineId());
                }
            } catch (Exception e) {
                log.error("🔍 获取基线内容失败: baselineId={}", compareResult.getBaselineId(), e);
            }
            
            // 获取当前内容
            String currentContent = "";
            try {
                if (compareResult.getCollectResultId() != null && compareResult.getCollectResultId() > 0) {
                    CollectResultEntity collectResult = collectResultEntityService.getById(compareResult.getCollectResultId());
                    if (collectResult != null) {
                        currentContent = collectResult.getCollectContent();
                        log.info("🔍 获取当前内容成功: collectResultId={}, 内容长度={}",
                                compareResult.getCollectResultId(),
                                currentContent != null ? currentContent.length() : 0);
                        log.debug("🔍 当前内容前500字符: {}",
                                currentContent != null && currentContent.length() > 500 ?
                                currentContent.substring(0, 500) + "..." : currentContent);
                    } else {
                        log.warn("🔍 采集结果不存在: collectResultId={}", compareResult.getCollectResultId());
                    }
                } else {
                    log.warn("🔍 采集结果ID为空: collectResultId={}", compareResult.getCollectResultId());
                }
            } catch (Exception e) {
                log.error("🔍 获取当前内容失败: collectResultId={}", compareResult.getCollectResultId(), e);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", diffPage.getRecords());
            result.put("total", diffPage.getTotal());
            result.put("current", diffPage.getCurrent());
            result.put("size", diffPage.getSize());
            result.put("baselineContent", baselineContent);
            result.put("currentContent", currentContent);
            
            // 【新增】重新生成 alignedLines 数据用于前端显示
            List<Object> alignedLines = null;
            if (!baselineContent.isEmpty() && !currentContent.isEmpty()) {
                try {
                    CompareContext context = new CompareContext();
                    context.setBaselineContent(baselineContent);
                    context.setCurrentContent(currentContent);
                    context.setContentType("TEXT"); // 设置内容类型为TEXT
                    
                    // 执行比对算法获取完整结果(包括 alignedLines)
                    CompareResultModel compareModel = compareAlgorithmManager.getAlgorithm("TEXT").compare(context);
                    
                    if (compareModel != null && compareModel.getAlignedLines() != null) {
                        alignedLines = new ArrayList<>(compareModel.getAlignedLines());
                        log.info("✅ 生成对齐行数据: {} 行", alignedLines.size());
                        
                        // 【调试】打印前20行的对齐信息
                        for (int idx = 0; idx < Math.min(20, alignedLines.size()); idx++) {
                            AlignedLine line = (AlignedLine) alignedLines.get(idx);
                            log.info("行{}: baseline[{}]=「{}」, current[{}]=「{}」, type={}", 
                                idx + 1,
                                line.getBaselineLineNumber(), 
                                line.getBaselineContent() != null && line.getBaselineContent().length() > 30 
                                    ? line.getBaselineContent().substring(0, 30) + "..." 
                                    : line.getBaselineContent(),
                                line.getCurrentLineNumber(),
                                line.getCurrentContent() != null && line.getCurrentContent().length() > 30
                                    ? line.getCurrentContent().substring(0, 30) + "..."
                                    : line.getCurrentContent(),
                                line.getDiffType()
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ 生成对齐行数据失败", e);
                }
            }
            result.put("alignedLines", alignedLines != null ? alignedLines : new ArrayList<>());
            
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取差异详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "导出比对结果")
    @PostMapping("/results/export")
    public void exportResults(@RequestBody Map<String, Object> params, HttpServletResponse response) {
        try {
            // TODO: 实现导出比对结果，需要CompareResultService
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=compare_results.xlsx");
        } catch (Exception e) {
            log.error("导出比对结果失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取结果统计")
    @GetMapping("/results/statistics")
    public Result<Object> getResultStatistics(
            @RequestParam(required = false) Long systemId,
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            LambdaQueryWrapper<CompareResult> queryWrapper = new LambdaQueryWrapper<>();
            
            // 添加taskId条件
            if (taskId != null) {
                queryWrapper.eq(CompareResult::getTaskId, taskId);
            }
            
            // 添加时间范围条件
            if (startTime != null && !startTime.trim().isEmpty()) {
                queryWrapper.ge(CompareResult::getExecuteTime, LocalDateTime.parse(startTime));
            }
            
            if (endTime != null && !endTime.trim().isEmpty()) {
                queryWrapper.le(CompareResult::getExecuteTime, LocalDateTime.parse(endTime));
            }
            
            // 统计总数
            long totalResults = compareResultService.count(queryWrapper);
            
            // 统计一致的结果
            LambdaQueryWrapper<CompareResult> consistentWrapper = new LambdaQueryWrapper<>();
            consistentWrapper.eq(CompareResult::getCompareStatus, 1);
            if (taskId != null) {
                consistentWrapper.eq(CompareResult::getTaskId, taskId);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                consistentWrapper.ge(CompareResult::getExecuteTime, LocalDateTime.parse(startTime));
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                consistentWrapper.le(CompareResult::getExecuteTime, LocalDateTime.parse(endTime));
            }
            long consistentResults = compareResultService.count(consistentWrapper);
            
            // 统计不一致的结果
            LambdaQueryWrapper<CompareResult> inconsistentWrapper = new LambdaQueryWrapper<>();
            inconsistentWrapper.eq(CompareResult::getCompareStatus, 0);
            if (taskId != null) {
                inconsistentWrapper.eq(CompareResult::getTaskId, taskId);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                inconsistentWrapper.ge(CompareResult::getExecuteTime, LocalDateTime.parse(startTime));
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                inconsistentWrapper.le(CompareResult::getExecuteTime, LocalDateTime.parse(endTime));
            }
            long inconsistentResults = compareResultService.count(inconsistentWrapper);
            
            // 统计失败的结果
            LambdaQueryWrapper<CompareResult> failedWrapper = new LambdaQueryWrapper<>();
            failedWrapper.eq(CompareResult::getCompareStatus, -1);
            if (taskId != null) {
                failedWrapper.eq(CompareResult::getTaskId, taskId);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                failedWrapper.ge(CompareResult::getExecuteTime, LocalDateTime.parse(startTime));
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                failedWrapper.le(CompareResult::getExecuteTime, LocalDateTime.parse(endTime));
            }
            long failedResults = compareResultService.count(failedWrapper);
            
            // 计算一致性率
            double consistencyRate = totalResults > 0 ? (double) consistentResults / totalResults * 100 : 0.0;
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", totalResults);
            result.put("consistentCount", consistentResults);
            result.put("inconsistentCount", inconsistentResults);
            result.put("failedCount", failedResults);
            result.put("consistencyRate", Math.round(consistencyRate * 100.0) / 100.0);
            
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取结果统计失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 比对执行相关接口 ====================

    @Operation(summary = "获取比对执行列表")
    @GetMapping("/executions")
    public Result<Object> getExecutionList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // TODO: 实现比对执行列表查询，需要CompareExecutionService
            return Result.success("查询成功", Map.of("records", List.of(), "total", 0));
        } catch (Exception e) {
            log.error("获取比对执行列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取执行详情")
    @GetMapping("/executions/{id}")
    public Result<Object> getExecutionById(@PathVariable Long id) {
        try {
            // TODO: 实现获取执行详情，需要CompareExecutionService
            return Result.success("查询成功", Map.of("id", id, "status", "运行中"));
        } catch (Exception e) {
            log.error("获取执行详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "停止执行")
    @PostMapping("/executions/{id}/stop")
    public Result<Void> stopExecution(@PathVariable Long id) {
        try {
            // TODO: 实现停止执行，需要CompareExecutionService
            return Result.success("停止成功");
        } catch (Exception e) {
            log.error("停止执行失败", e);
            return Result.error("停止失败：" + e.getMessage());
        }
    }

    @Operation(summary = "重新执行")
    @PostMapping("/executions/{id}/retry")
    public Result<Void> retryExecution(@PathVariable Long id) {
        try {
            // TODO: 实现重新执行，需要CompareExecutionService
            return Result.success("重新执行成功");
        } catch (Exception e) {
            log.error("重新执行失败", e);
            return Result.error("重新执行失败：" + e.getMessage());
        }
    }

    // ==================== SSH文本比对相关接口 ====================

    @Operation(summary = "执行SSH文本比对")
    @PostMapping("/ssh-text-compare")
    public Result<Object> executeSshTextCompare(@RequestBody Map<String, Object> request) {
        try {
            log.info("收到SSH文本比对请求: {}", request);
            
            // 解析请求参数
            Long taskId = Long.valueOf(request.get("taskId").toString());
            String baselineContent = (String) request.get("baselineContent");
            List<String> collectedContents = (List<String>) request.get("collectedContents");
            
            // 构建比对规则
            CompareRule compareRule = new CompareRule();
            compareRule.setCompareType("ssh_text");
            compareRule.setSshCompareMode((String) request.getOrDefault("compareMode", "line_by_line"));
            compareRule.setIgnoreLines((String) request.getOrDefault("ignoreLines", ""));
            compareRule.setDiffThreshold(Double.valueOf(request.getOrDefault("diffThreshold", "5.0").toString()));
            
            // 解析忽略差异选项
            List<String> ignoreDifferences = (List<String>) request.getOrDefault("ignoreDifferences", List.of("whitespace"));
            try {
                compareRule.setIgnoreDifferences(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(ignoreDifferences));
            } catch (Exception e) {
                log.warn("解析忽略差异选项失败", e);
                compareRule.setIgnoreDifferences("[\"whitespace\"]");
            }
            
            // 执行比对
            // TODO: 需要CompareTaskService提供executeSshTextCompare方法
            // CompareExecution execution = compareTaskService.executeSshTextCompare(
            //     taskId, compareRule, baselineContent, collectedContents
            // );
            
            return Result.success("SSH文本比对执行成功", Map.of("executeId", "COMPARE_" + System.currentTimeMillis()));
            
        } catch (Exception e) {
            log.error("执行SSH文本比对失败", e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }

    @Operation(summary = "测试SSH文本比对")
    @PostMapping("/ssh-text-compare/test")
    public Result<Object> testSshTextCompare(@RequestBody Map<String, Object> request) {
        try {
            log.info("收到SSH文本比对测试请求: {}", request);
            
            String baselineContent = (String) request.get("baselineContent");
            String collectedContent = (String) request.get("collectedContent");
            String compareMode = (String) request.getOrDefault("compareMode", "line_by_line");
            String ignoreLines = (String) request.getOrDefault("ignoreLines", "");
            Double diffThreshold = Double.valueOf(request.getOrDefault("diffThreshold", "5.0").toString());
            List<String> ignoreDifferences = (List<String>) request.getOrDefault("ignoreDifferences", List.of("whitespace"));
            
            // 构建比对规则
            CompareRule compareRule = new CompareRule();
            compareRule.setCompareType("ssh_text");
            compareRule.setSshCompareMode(compareMode);
            compareRule.setIgnoreLines(ignoreLines);
            compareRule.setDiffThreshold(diffThreshold);
            
            try {
                compareRule.setIgnoreDifferences(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(ignoreDifferences));
            } catch (Exception e) {
                log.warn("解析忽略差异选项失败", e);
                compareRule.setIgnoreDifferences("[\"whitespace\"]");
            }
            
            // 执行测试比对
            // TODO: 需要CompareTaskService提供executeSshTextCompare方法
            // CompareExecution execution = compareTaskService.executeSshTextCompare(
            //     0L, compareRule, baselineContent, List.of(collectedContent)
            // );
            
            return Result.success("SSH文本比对测试成功", Map.of("executeId", "COMPARE_" + System.currentTimeMillis()));
            
        } catch (Exception e) {
            log.error("SSH文本比对测试失败", e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }
}
