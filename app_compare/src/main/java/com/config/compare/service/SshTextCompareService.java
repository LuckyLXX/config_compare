package com.config.compare.service;

import com.config.compare.entity.CompareResult;

/**
 * SSH文本比对服务接口
 */
public interface SshTextCompareService {

    /**
     * 执行SSH文本比对
     *
     * @param baselineContent 基线配置内容
     * @param collectedContent 采集配置内容
     * @param compareMode 比对方式：line_by_line, ignore_whitespace, ignore_case, smart_diff
     * @param ignoreLines 忽略行关键词（逗号分隔）
     * @param diffThreshold 差异阈值（百分比）
     * @param ignoreDifferences 忽略差异选项
     * @return 比对结果
     */
    CompareResult compareSshText(String baselineContent, 
                                String collectedContent, 
                                String compareMode, 
                                String ignoreLines, 
                                Double diffThreshold, 
                                String[] ignoreDifferences);

    /**
     * 逐行比对
     *
     * @param baselineLines 基线内容行数组
     * @param collectedLines 采集内容行数组
     * @param ignoreLines 忽略行关键词
     * @return 比对结果
     */
    CompareResult lineByLineCompare(String[] baselineLines, 
                                   String[] collectedLines, 
                                   String[] ignoreLines);

    /**
     * 忽略空白字符比对
     *
     * @param baselineContent 基线内容
     * @param collectedContent 采集内容
     * @return 比对结果
     */
    CompareResult ignoreWhitespaceCompare(String baselineContent, 
                                        String collectedContent);

    /**
     * 忽略大小写比对
     *
     * @param baselineContent 基线内容
     * @param collectedContent 采集内容
     * @return 比对结果
     */
    CompareResult ignoreCaseCompare(String baselineContent, 
                                   String collectedContent);

    /**
     * 智能比对
     *
     * @param baselineContent 基线内容
     * @param collectedContent 采集内容
     * @param ignoreLines 忽略行关键词
     * @return 比对结果
     */
    CompareResult smartDiffCompare(String baselineContent, 
                                  String collectedContent, 
                                  String[] ignoreLines);
}



