package com.config.compare.service.impl;

import com.config.compare.entity.CompareResult;
import com.config.compare.service.CompareReportService;
import com.config.compare.service.CompareResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 比对报告Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-09-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompareReportServiceImpl implements CompareReportService {

    private final CompareResultService compareResultService;

    @Override
    public ByteArrayOutputStream generateExcelReport(Map<String, Object> exportData) {
        try {
            log.info("开始生成Excel报告");
            
            String systemName = (String) exportData.get("systemName");
            Map<String, Object> overview = (Map<String, Object>) exportData.get("overview");
            List<Map<String, Object>> compareResults = (List<Map<String, Object>>) exportData.get("compareResults");
            
            // 创建工作簿
            Workbook workbook = new XSSFWorkbook();
            
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 第一个工作表：概览统计
            Sheet overviewSheet = workbook.createSheet("概览统计");
            createOverviewSheet(overviewSheet, systemName, overview, headerStyle, dataStyle);
            
            // 第二个工作表：比对结果明细
            Sheet detailSheet = workbook.createSheet("比对结果明细");
            createDetailSheet(detailSheet, compareResults, headerStyle, dataStyle);
            
            // 写入输出流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            log.info("Excel报告生成完成");
            return outputStream;
            
        } catch (Exception e) {
            log.error("生成Excel报告失败", e);
            throw new RuntimeException("生成Excel报告失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> getReportData(Long systemId) {
        try {
            log.info("获取报告数据，系统ID: {}", systemId);
            
            // 获取该系统的所有比对结果
            // TODO: 根据systemId过滤实际结果，当前返回全部以保持兼容
            List<CompareResult> results = compareResultService.list();
            
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("systemId", systemId);
            reportData.put("compareResults", results);
            
            return reportData;
            
        } catch (Exception e) {
            log.error("获取报告数据失败", e);
            throw new RuntimeException("获取报告数据失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 创建概览统计工作表
     */
    private void createOverviewSheet(Sheet sheet, String systemName, Map<String, Object> overview, 
                                     CellStyle headerStyle, CellStyle dataStyle) {
        // 标题行
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(systemName + " - 配置比对报告");
        titleCell.setCellStyle(headerStyle);
        
        // 生成时间
        Row timeRow = sheet.createRow(1);
        timeRow.createCell(0).setCellValue("生成时间：");
        timeRow.createCell(1).setCellValue(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 空行
        sheet.createRow(2);
        
        // 统计数据表头
        Row headerRow = sheet.createRow(3);
        String[] headers = {"指标", "数量", "占比(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 统计数据
        if (overview != null) {
            String[][] stats = {
                {"配置一致", String.valueOf(getIntValue(overview, "consistentCount")), String.format("%.1f", getDoubleValue(overview, "consistentRate"))},
                {"配置不一致", String.valueOf(getIntValue(overview, "inconsistentCount")), String.format("%.1f", getDoubleValue(overview, "inconsistentRate"))},
                {"配置缺失", String.valueOf(getIntValue(overview, "missingCount")), String.format("%.1f", getDoubleValue(overview, "missingRate"))},
                {"多余配置", String.valueOf(getIntValue(overview, "extraCount")), String.format("%.1f", getDoubleValue(overview, "extraRate"))}
            };
            
            for (int i = 0; i < stats.length; i++) {
                Row dataRow = sheet.createRow(4 + i);
                for (int j = 0; j < stats[i].length; j++) {
                    Cell cell = dataRow.createCell(j);
                    cell.setCellValue(stats[i][j]);
                    cell.setCellStyle(dataStyle);
                }
            }
        }
        
        // 自动调整列宽
        for (int i = 0; i < 3; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }
    
    /**
     * 创建比对结果明细工作表
     */
    private void createDetailSheet(Sheet sheet, List<Map<String, Object>> compareResults, 
                                   CellStyle headerStyle, CellStyle dataStyle) {
        // 表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"任务名称", "服务器实例", "比对状态", "一致性得分", "差异总数", "新增配置", "配置缺失", "配置不一致", "执行时间"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 数据行
        if (compareResults != null) {
            int rowNum = 1;
            for (Map<String, Object> result : compareResults) {
                Row dataRow = sheet.createRow(rowNum++);
                
                dataRow.createCell(0).setCellValue(String.valueOf(result.get("taskName")));
                dataRow.createCell(1).setCellValue(String.valueOf(result.get("serverInstance")));
                
                int compareStatus = getIntValue(result, "compareStatus");
                dataRow.createCell(2).setCellValue(compareStatus == 1 ? "一致" : "不一致");
                
                dataRow.createCell(3).setCellValue(String.valueOf(result.get("consistencyScore")));
                dataRow.createCell(4).setCellValue(String.valueOf(result.get("diffCount")));
                dataRow.createCell(5).setCellValue(String.valueOf(result.getOrDefault("addCount", 0)));
                dataRow.createCell(6).setCellValue(String.valueOf(result.getOrDefault("deleteCount", 0)));
                dataRow.createCell(7).setCellValue(String.valueOf(result.getOrDefault("modifyCount", 0)));
                dataRow.createCell(8).setCellValue(String.valueOf(result.get("executeTime")));
                
                // 应用样式
                for (int i = 0; i < headers.length; i++) {
                    dataRow.getCell(i).setCellStyle(dataStyle);
                }
            }
        }
        
        // 自动调整列宽
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
    }
    
    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        return style;
    }
    
    /**
     * 创建数据样式
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    /**
     * 获取整数值
     */
    private int getIntValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Double) return ((Double) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 获取Double值
     */
    private double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
    
    
}
