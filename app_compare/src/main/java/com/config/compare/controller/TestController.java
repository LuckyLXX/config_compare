package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.TaskSchedulerService;
import com.config.compare.service.impl.TaskSchedulerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 测试控制器
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "测试接口")
public class TestController {

    private final TaskSchedulerServiceImpl taskSchedulerService;

    @Operation(summary = "测试定时任务功能")
    @GetMapping("/scheduler")
    public Result<String> testScheduler() {
        try {
            // 获取所有定时采集任务
            var collectTasks = taskSchedulerService.getScheduledCollectTasks();
            log.info("定时采集任务数量: {}", collectTasks.size());
            for (var task : collectTasks) {
                log.info("采集任务: {} (ID: {}), Cron: {}, 状态: {}", 
                    task.getTaskName(), task.getId(), task.getCronExpression(), task.getStatus());
            }
            
            // 获取所有定时比对任务
            var compareTasks = taskSchedulerService.getScheduledCompareTasks();
            log.info("定时比对任务数量: {}", compareTasks.size());
            for (var task : compareTasks) {
                log.info("比对任务: {} (ID: {}), Cron: {}, 状态: {}", 
                    task.getTaskName(), task.getId(), task.getCronExpression(), task.getStatus());
            }
            
            return Result.success("定时任务测试完成，请查看日志");
        } catch (Exception e) {
            log.error("测试定时任务失败", e);
            return Result.error("测试失败：" + e.getMessage());
        }
    }

    @Operation(summary = "验证Cron表达式")
    @PostMapping("/validate-cron")
    public Result<Boolean> validateCron(@RequestParam String cronExpression) {
        try {
            boolean isValid = taskSchedulerService.validateCronExpression(cronExpression);
            return Result.success("验证完成", isValid);
        } catch (Exception e) {
            log.error("验证Cron表达式失败", e);
            return Result.error("验证失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取下次执行时间")
    @PostMapping("/next-execution")
    public Result<String> getNextExecution(@RequestParam String cronExpression) {
        try {
            String nextTime = taskSchedulerService.getNextExecutionTime(cronExpression);
            return Result.success("计算完成", nextTime);
        } catch (Exception e) {
            log.error("获取下次执行时间失败", e);
            return Result.error("计算失败：" + e.getMessage());
        }
    }
}
