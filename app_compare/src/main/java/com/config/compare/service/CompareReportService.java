package com.config.compare.service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * 比对报告Service接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-09-30
 */
public interface CompareReportService {

    // 已移除PDF报告导出

    /**
     * 生成Excel报告
     * 
     * @param exportData 导出数据
     * @return Excel字节流
     */
    ByteArrayOutputStream generateExcelReport(Map<String, Object> exportData);

    /**
     * 获取报告数据
     * 
     * @param systemId 系统ID
     * @return 报告数据
     */
    Map<String, Object> getReportData(Long systemId);
}
