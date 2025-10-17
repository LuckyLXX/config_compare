package com.config.compare.service;

import java.util.Map;

/**
 * 报告执行服务接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ReportExecutionService {

    /**
     * 获取执行概览数据
     * 
     * @return 执行概览数据
     */
    Map<String, Object> getExecutionOverview();

    /**
     * 获取执行报告列表
     * 
     * @param current 页码
     * @param size 页大小
     * @param reportType 报告类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 执行报告列表
     */
    Map<String, Object> getExecutionReports(Integer current, Integer size, 
                                          String reportType, String startTime, String endTime);

    /**
     * 生成执行报告
     * 
     * @param request 请求参数
     * @return 生成结果
     */
    Map<String, Object> generateExecutionReport(Map<String, Object> request);

    /**
     * 导出执行报告
     * 
     * @param params 导出参数
     * @return 导出结果
     */
    Map<String, Object> exportExecutionReport(Map<String, Object> params);

    /**
     * 获取报告详情
     * 
     * @param id 报告ID
     * @return 报告详情
     */
    Map<String, Object> getExecutionReportById(Long id);

    /**
     * 删除执行报告
     * 
     * @param id 报告ID
     * @return 删除结果
     */
    boolean deleteExecutionReport(Long id);
}