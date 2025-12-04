package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.TaskSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 工具类Controller
 * 提供通用的工具接口，如Cron表达式验证等
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@RestController
@RequestMapping("/utils")
@RequiredArgsConstructor
@Tag(name = "工具接口")
@Validated
public class UtilController {

    private final TaskSchedulerService taskSchedulerService;

    @Operation(summary = "验证Cron表达式")
    @PostMapping("/validate-cron")
    public Result<Boolean> validateCronExpression(@RequestBody Map<String, String> request) {
        try {
            String cronExpression = request.get("cronExpression");
            boolean isValid = taskSchedulerService.validateCronExpression(cronExpression);
            return Result.success("验证完成", isValid);
        } catch (Exception e) {
            log.error("验证Cron表达式失败", e);
            return Result.error("验证失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取下次执行时间")
    @PostMapping("/next-execution")
    public Result<String> getNextExecutionTime(@RequestBody Map<String, String> request) {
        try {
            String cronExpression = request.get("cronExpression");
            String nextTime = taskSchedulerService.getNextExecutionTime(cronExpression);
            return Result.success("计算完成", nextTime);
        } catch (Exception e) {
            log.error("获取下次执行时间失败", e);
            return Result.error("计算失败：" + e.getMessage());
        }
    }
}

