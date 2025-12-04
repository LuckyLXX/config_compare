package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.DataProcessService;
import com.config.compare.service.dto.AiProcessRequest;
import com.config.compare.service.dto.DataCleanRequest;
import com.config.compare.service.dto.DataProcessResponse;
import com.config.compare.service.dto.ExcelConvertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

/**
 * 数据处理Controller
 * 
 * 提供JSON转Excel、AI智能处理、数据清洗等数据处理功能
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
@Slf4j
@RestController
@RequestMapping("/data-process")
@RequiredArgsConstructor
@Tag(name = "数据处理", description = "数据处理中心相关接口")
@Validated
public class DataProcessController {

    private final DataProcessService dataProcessService;

    /**
     * JSON转Excel
     */
    @Operation(summary = "JSON转Excel", description = "将JSON数据转换为Excel文件")
    @PostMapping("/excel/convert")
    public Result<DataProcessResponse> convertToExcel(@Valid @RequestBody ExcelConvertRequest request) {
        try {
            log.info("收到JSON转Excel请求，模式: {}", request.getMode());
            DataProcessResponse response = dataProcessService.convertToExcel(request);
            
            if (response.getSuccess()) {
                return Result.success("转换成功", response);
            } else {
                return Result.error(response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("JSON转Excel失败", e);
            return Result.error("转换失败: " + e.getMessage());
        }
    }

    /**
     * AI智能处理
     */
    @Operation(summary = "AI智能处理", description = "使用AI模型处理和分析数据")
    @PostMapping("/ai/process")
    public Result<DataProcessResponse> aiProcess(@Valid @RequestBody AiProcessRequest request) {
        try {
            log.info("收到AI处理请求，模型: {}, 指令: {}", request.getModel(), request.getPrompt());
            DataProcessResponse response = dataProcessService.aiProcess(request);
            
            if (response.getSuccess()) {
                return Result.success("处理成功", response);
            } else {
                return Result.error(response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("AI处理失败", e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 数据清洗
     */
    @Operation(summary = "数据清洗", description = "对JSON数据进行清洗处理")
    @PostMapping("/clean")
    public Result<DataProcessResponse> cleanData(@Valid @RequestBody DataCleanRequest request) {
        try {
            log.info("收到数据清洗请求，规则: {}", request.getRules());
            DataProcessResponse response = dataProcessService.cleanData(request);
            
            if (response.getSuccess()) {
                return Result.success("清洗成功", response);
            } else {
                return Result.error(response.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("数据清洗失败", e);
            return Result.error("清洗失败: " + e.getMessage());
        }
    }

    /**
     * 下载处理结果文件
     */
    @Operation(summary = "下载文件", description = "下载处理生成的文件")
    @GetMapping("/download/{fileId}")
    public void downloadFile(
            @Parameter(description = "文件ID") @PathVariable String fileId,
            HttpServletResponse response) {
        try {
            log.info("收到文件下载请求，fileId: {}", fileId);
            dataProcessService.downloadFile(fileId, response);
        } catch (Exception e) {
            log.error("文件下载失败", e);
            try {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(500);
                response.getWriter().write("{\"code\":500,\"message\":\"" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("响应错误信息失败", ex);
            }
        }
    }

    /**
     * 获取文件信息
     */
    @Operation(summary = "获取文件信息", description = "获取处理生成的文件信息")
    @GetMapping("/file/{fileId}")
    public Result<String> getFileInfo(@Parameter(description = "文件ID") @PathVariable String fileId) {
        try {
            String filePath = dataProcessService.getFilePath(fileId);
            if (filePath != null) {
                return Result.success("查询成功", filePath);
            } else {
                return Result.error("文件不存在或已过期");
            }
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 清理过期文件
     */
    @Operation(summary = "清理过期文件", description = "清理指定时间前生成的临时文件")
    @DeleteMapping("/files/expired")
    public Result<Integer> cleanExpiredFiles(
            @Parameter(description = "过期时间（分钟）") @RequestParam(defaultValue = "60") Integer expireMinutes) {
        try {
            int count = dataProcessService.cleanExpiredFiles(expireMinutes);
            return Result.success("清理完成", count);
        } catch (Exception e) {
            log.error("清理过期文件失败", e);
            return Result.error("清理失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据处理任务列表（关联最新采集结果）
     */
    @Operation(summary = "获取数据处理任务列表", description = "查询每个任务的最新采集结果，分页展示")
    @GetMapping("/tasks")
    public Result<Object> getDataProcessTasks(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "任务名称") @RequestParam(required = false) String taskName,
            @Parameter(description = "采集类型") @RequestParam(required = false) String collectType,
            @Parameter(description = "系统ID") @RequestParam(required = false) Long systemId) {
        try {
            log.info("查询数据处理任务列表, current={}, size={}", current, size);
            var result = dataProcessService.getDataProcessTasks(current, size, taskName, collectType, systemId);
            return Result.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询数据处理任务列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 根据执行ID获取采集结果详情
     */
    @Operation(summary = "获取采集结果详情", description = "根据执行ID获取完整的采集结果内容")
    @GetMapping("/result/{executeId}")
    public Result<Object> getCollectResult(
            @Parameter(description = "执行ID") @PathVariable String executeId) {
        try {
            log.info("查询采集结果详情, executeId={}", executeId);
            var result = dataProcessService.getCollectResultByExecuteId(executeId);
            if (result != null) {
                return Result.success("查询成功", result);
            } else {
                return Result.error("采集结果不存在");
            }
        } catch (Exception e) {
            log.error("查询采集结果详情失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 测试AI连接
     */
    @Operation(summary = "测试AI连接", description = "测试AI服务的连通性")
    @PostMapping("/ai/test-connection")
    public Result<Object> testAiConnection(@RequestBody java.util.Map<String, Object> config) {
        try {
            String url = (String) config.get("url");
            String apiKey = (String) config.get("apiKey");
            String modelId = (String) config.get("modelId");
            Integer timeout = config.get("timeout") != null ? Integer.parseInt(config.get("timeout").toString()) : 30;
            
            log.info("测试AI连接, url={}, modelId={}", url, modelId);
            var result = dataProcessService.testAiConnection(url, apiKey, modelId, timeout);
            return Result.success("测试完成", result);
        } catch (Exception e) {
            log.error("测试AI连接失败", e);
            return Result.error("测试失败: " + e.getMessage());
        }
    }
}
