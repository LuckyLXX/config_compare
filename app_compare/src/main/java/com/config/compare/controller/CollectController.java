package com.config.compare.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.common.result.Result;
import com.config.compare.entity.CollectTask;
import com.config.compare.entity.CollectTemplate;
import com.config.compare.service.CollectExecutionService;
import com.config.compare.service.CollectTaskService;
import com.config.compare.service.CollectTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 采集管理Controller（简化版本）
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/collect")
@RequiredArgsConstructor
@Tag(name = "采集管理")
@Validated
public class CollectController {

    private final CollectTemplateService collectTemplateService;
    private final CollectTaskService collectTaskService;
    private final CollectExecutionService collectExecutionService;

    // ==================== 采集模板相关接口 ====================
    
    @Operation(summary = "获取采集模板列表")
    @GetMapping("/templates")
    public Result<IPage<CollectTemplate>> getTemplateList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "模板名称") @RequestParam(required = false) String templateName,
            @Parameter(description = "模板类型") @RequestParam(required = false) String templateType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        try {
            IPage<CollectTemplate> result = collectTemplateService.getTemplateList(current, size, templateName, templateType, status);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("获取采集模板列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建采集模板")
    @PostMapping("/templates")
    public Result<Void> createTemplate(@Valid @RequestBody CollectTemplate template) {
        try {
            collectTemplateService.save(template);
            return Result.success("创建成功");
        } catch (Exception e) {
            log.error("创建采集模板失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新采集模板")
    @PutMapping("/templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @Valid @RequestBody CollectTemplate template) {
        try {
            template.setId(id);
            collectTemplateService.updateById(template);
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新采集模板失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除采集模板")
    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        try {
            collectTemplateService.removeById(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除采集模板失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取采集模板详情")
    @GetMapping("/templates/{id}")
    public Result<CollectTemplate> getTemplateById(@PathVariable Long id) {
        try {
            CollectTemplate template = collectTemplateService.getById(id);
            if (template == null) {
                return Result.error("模板不存在");
            }
            return Result.success("查询成功", template);
        } catch (Exception e) {
            log.error("获取采集模板详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "测试采集模板")
    @PostMapping("/templates/{id}/test")
    public Result<Map<String, Object>> testTemplate(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Long serverId = Long.valueOf(request.get("serverId").toString());
            
            Map<String, Object> testResult;
            
            // 如果请求中包含配置信息，则使用动态配置测试
            if (request.containsKey("templateType") && request.containsKey("config")) {
                String templateType = request.get("templateType").toString();
                String templateContent = request.get("config").toString();
                testResult = collectTemplateService.testTemplateWithConfig(templateType, templateContent, serverId);
            } else {
                // 否则使用已保存的模板测试
                testResult = collectTemplateService.testTemplate(id, serverId);
            }
            
            return Result.success("测试完成", testResult);
        } catch (Exception e) {
            log.error("测试采集模板失败", e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据服务器类型获取适用模板")
    @GetMapping("/templates/by-server-type/{serverTypeId}")
    public Result<List<CollectTemplate>> getTemplatesByServerType(@PathVariable Long serverTypeId) {
        try {
            List<CollectTemplate> templates = collectTemplateService.getTemplatesByServerType(serverTypeId);
            return Result.success("查询成功", templates);
        } catch (Exception e) {
            log.error("根据服务器类型获取模板失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 采集任务相关接口 ====================

    @Operation(summary = "获取采集任务列表")
    @GetMapping("/tasks")
    public Result<Object> getTaskList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "系统ID") @RequestParam(required = false) Long systemId,
            @Parameter(description = "执行类型") @RequestParam(required = false) Integer executeType) {
        try {
            IPage<Map<String, Object>> page = collectTaskService.getTaskListWithDetails(current, size, taskName, systemId, executeType);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("获取采集任务列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "创建采集任务")
    @PostMapping("/tasks")
    public Result<Void> createTask(@RequestBody @Valid CollectTask task) {
        try {
            boolean success = collectTaskService.createTask(task);
            return success ? Result.success("创建成功") : Result.error("创建失败");
        } catch (Exception e) {
            log.error("创建采集任务失败", e);
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    @Operation(summary = "更新采集任务")
    @PutMapping("/tasks/{id}")
    public Result<Void> updateTask(@PathVariable Long id, @RequestBody @Valid CollectTask task) {
        try {
            task.setId(id);
            boolean success = collectTaskService.updateTask(task);
            return success ? Result.success("更新成功") : Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新采集任务失败", e);
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "删除采集任务")
    @DeleteMapping("/tasks/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        try {
            boolean success = collectTaskService.removeById(id);
            return success ? Result.success("删除成功") : Result.error("删除失败");
        } catch (Exception e) {
            log.error("删除采集任务失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据ID获取采集任务详情")
    @GetMapping("/tasks/{id}")
    public Result<Object> getTaskById(@PathVariable Long id) {
        try {
            Map<String, Object> task = collectTaskService.getTaskWithDetails(id);
            return task != null ? Result.success("查询成功", task) : Result.error("任务不存在");
        } catch (Exception e) {
            log.error("获取采集任务详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "启用/禁用采集任务")
    @PutMapping("/tasks/{id}/status")
    public Result<Void> toggleTaskStatus(@PathVariable Long id, @RequestBody Map<String, Integer> request) {
        try {
            Integer status = request.get("status");
            boolean success = collectTaskService.toggleTaskStatus(id, status);
            return success ? Result.success("状态更新成功") : Result.error("状态更新失败");
        } catch (Exception e) {
            log.error("更新采集任务状态失败", e);
            return Result.error("状态更新失败：" + e.getMessage());
        }
    }

    @Operation(summary = "立即执行采集任务")
    @PostMapping("/tasks/{id}/execute")
    public Result<String> executeTask(@PathVariable Long id) {
        try {
            String executionId = collectTaskService.executeTask(id);
            return Result.success("任务执行成功", executionId);
        } catch (Exception e) {
            log.error("执行采集任务失败", e);
            return Result.error("执行失败：" + e.getMessage());
        }
    }

    @Operation(summary = "批量执行采集任务")
    @PostMapping("/tasks/batch-execute")
    public Result<Void> batchExecute(@RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> taskIds = request.get("taskIds");
            boolean success = collectTaskService.batchExecute(taskIds);
            return success ? Result.success("批量执行成功") : Result.error("批量执行失败");
        } catch (Exception e) {
            log.error("批量执行采集任务失败", e);
            return Result.error("批量执行失败：" + e.getMessage());
        }
    }

    @Operation(summary = "暂停采集任务")
    @PostMapping("/tasks/{id}/pause")
    public Result<Void> pauseTask(@PathVariable Long id) {
        try {
            boolean success = collectTaskService.pauseTask(id);
            return success ? Result.success("暂停成功") : Result.error("暂停失败");
        } catch (Exception e) {
            log.error("暂停采集任务失败", e);
            return Result.error("暂停失败：" + e.getMessage());
        }
    }

    @Operation(summary = "恢复采集任务")
    @PostMapping("/tasks/{id}/resume")
    public Result<Void> resumeTask(@PathVariable Long id) {
        try {
            boolean success = collectTaskService.resumeTask(id);
            return success ? Result.success("恢复成功") : Result.error("恢复失败");
        } catch (Exception e) {
            log.error("恢复采集任务失败", e);
            return Result.error("恢复失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取任务执行历史")
    @GetMapping("/tasks/{taskId}/executions")
    public Result<Object> getTaskExecutions(@PathVariable Long taskId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<Map<String, Object>> page = collectTaskService.getTaskExecutions(taskId, current, size);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("获取任务执行历史失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取任务执行结果")
    @GetMapping("/tasks/{taskId}/executions/{executionId}/results")
    public Result<Object> getTaskResults(@PathVariable Long taskId, @PathVariable String executionId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            IPage<Map<String, Object>> page = collectTaskService.getTaskResults(taskId, executionId, current, size);
            return Result.success("查询成功", page);
        } catch (Exception e) {
            log.error("获取任务执行结果失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 采集执行相关接口 ====================

    @Operation(summary = "获取采集执行列表")
    @GetMapping("/executions")
    public Result<Object> getExecutionList(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // 暂未使用，返回空列表
            return Result.success("查询成功", Map.of("records", List.of(), "total", 0));
        } catch (Exception e) {
            log.error("获取采集执行列表失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "根据执行ID获取执行详情")
    @GetMapping("/executions/{executeId}")
    public Result<Object> getExecutionById(@PathVariable String executeId) {
        try {
            var execution = collectExecutionService.getByExecuteId(executeId);
            if (execution == null) {
                return Result.error("执行记录不存在");
            }

            // 组装前端所需字段
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("taskId", execution.getTaskId());
            data.put("executeId", execution.getExecuteId());
            data.put("executeStatus", execution.getExecuteStatus());
            data.put("totalServers", execution.getTotalServers());
            data.put("successServers", execution.getSuccessServers());
            data.put("failedServers", execution.getFailedServers());
            data.put("startTime", execution.getStartTime());
            data.put("endTime", execution.getEndTime());
            data.put("durationMs", execution.getDurationMs());

            try {
                var task = collectTaskService.getById(execution.getTaskId());
                if (task != null) {
                    data.put("taskName", task.getTaskName());
                }
            } catch (Exception ignore) {}

            return Result.success("查询成功", data);
        } catch (Exception e) {
            log.error("获取执行详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Operation(summary = "停止执行")
    @PostMapping("/executions/{id}/stop")
    public Result<Void> stopExecution(@PathVariable Long id) {
        try {
            // TODO: 实现停止执行
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
            // TODO: 实现重新执行
            return Result.success("重新执行成功");
        } catch (Exception e) {
            log.error("重新执行失败", e);
            return Result.error("重新执行失败：" + e.getMessage());
        }
    }
}
