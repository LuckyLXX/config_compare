package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 仪表板Controller
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-08-26
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表板管理")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表板统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = dashboardService.getDashboardStats();
            return Result.success("查询成功", stats);
        } catch (Exception e) {
            log.error("获取仪表板统计数据失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}
