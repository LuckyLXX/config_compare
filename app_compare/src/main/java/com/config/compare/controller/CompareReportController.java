package com.config.compare.controller;

import com.config.compare.common.result.Result;
import com.config.compare.service.CompareReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 比对报告Controller
 * 提供比对报告的导出功能（PDF和Excel）
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-09-30
 */
@Slf4j
@RestController
@RequestMapping("/report/compare")
@RequiredArgsConstructor
@Tag(name = "比对报告导出")
@Validated
public class CompareReportController {

    private final CompareReportService compareReportService;

    // 已移除PDF导出接口

    /**
     * 容错解析：宽容处理前端Body编码/格式，统一转换为Map
     */
    private Map<String, Object> parseJsonBody(String body) throws IOException {
        if (body == null || body.isEmpty()) {
            return new HashMap<>();
        }
        // 强制以UTF-8解析，再用Jackson反序列化
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            return mapper.readValue(body.getBytes(StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            // 如果不是严格json，尝试去掉BOM或非法字符
            String cleaned = body.replace("\uFEFF", "");
            return mapper.readValue(cleaned.getBytes(StandardCharsets.UTF_8), Map.class);
        }
    }
    @Operation(summary = "导出比对报告（Excel）")
    @PostMapping(value = "/export-excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> exportCompareReportExcel(@RequestBody String body) {
        try {
            Map<String, Object> exportData = parseJsonBody(body);
            log.info("开始导出比对数据Excel，系统ID: {}", exportData.get("systemId"));
            
            // 生成Excel报告
            ByteArrayOutputStream outputStream = compareReportService.generateExcelReport(exportData);
            byte[] excelBytes = outputStream.toByteArray();
            
            // 构建文件名
            String systemName = (String) exportData.get("systemName");
            String filename = systemName + "_比对数据_" + System.currentTimeMillis() + ".xlsx";
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", encodedFilename);
            headers.setContentLength(excelBytes.length);
            
            log.info("比对数据Excel导出成功，文件大小: {} bytes", excelBytes.length);
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
                    
        } catch (Exception e) {
            log.error("导出比对数据Excel失败", e);
            throw new RuntimeException("导出Excel失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取报告数据")
    @GetMapping("/data/{systemId}")
    public Result<Map<String, Object>> getReportData(
            @Parameter(description = "系统ID") @PathVariable Long systemId) {
        try {
            log.info("获取报告数据，系统ID: {}", systemId);
            Map<String, Object> reportData = compareReportService.getReportData(systemId);
            return Result.success("查询成功", reportData);
        } catch (Exception e) {
            log.error("获取报告数据失败", e);
            return Result.error("获取报告数据失败: " + e.getMessage());
        }
    }
}
