package com.config.compare.service.impl;

import com.config.compare.entity.CompareResult;
import com.config.compare.service.SshTextCompareService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * SSH文本比对服务实现类
 */
@Slf4j
@Service
public class SshTextCompareServiceImpl implements SshTextCompareService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CompareResult compareSshText(String baselineContent, 
                                      String collectedContent, 
                                      String compareMode, 
                                      String ignoreLines, 
                                      Double diffThreshold, 
                                      String[] ignoreDifferences) {
        
        long startTime = System.currentTimeMillis();
        CompareResult result = new CompareResult();
        
        try {
            // 预处理内容
            String[] ignoreLineArray = ignoreLines != null ? ignoreLines.split(",") : new String[0];
            
            // 根据比对方式执行比对
            switch (compareMode) {
                case "line_by_line":
                    result = lineByLineCompare(
                        baselineContent.split("\n"), 
                        collectedContent.split("\n"), 
                        ignoreLineArray
                    );
                    break;
                case "ignore_whitespace":
                    result = ignoreWhitespaceCompare(baselineContent, collectedContent);
                    break;
                case "ignore_case":
                    result = ignoreCaseCompare(baselineContent, collectedContent);
                    break;
                case "smart_diff":
                    result = smartDiffCompare(baselineContent, collectedContent, ignoreLineArray);
                    break;
                default:
                    result = lineByLineCompare(
                        baselineContent.split("\n"), 
                        collectedContent.split("\n"), 
                        ignoreLineArray
                    );
            }
            
            // 应用差异阈值
            if (diffThreshold != null && result.getDiffPercentage() != null) {
                if (result.getDiffPercentage() <= diffThreshold) {
                    result.setStatus(1); // 成功（差异在阈值内）
                } else {
                    result.setStatus(3); // 有差异（超过阈值）
                }
            }
            
            // 设置执行时间
            long endTime = System.currentTimeMillis();
            result.setCompareTime(endTime - startTime);
            result.setCreateTime(LocalDateTime.now());
            result.setUpdateTime(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("SSH文本比对失败", e);
            result.setStatus(2); // 失败
            result.setErrorMessage(e.getMessage());
            result.setCreateTime(LocalDateTime.now());
            result.setUpdateTime(LocalDateTime.now());
        }
        
        return result;
    }

    @Override
    public CompareResult lineByLineCompare(String[] baselineLines, 
                                         String[] collectedLines, 
                                         String[] ignoreLines) {
        CompareResult result = new CompareResult();
        
        try {
            // 过滤忽略行
            List<String> filteredBaselineLines = filterIgnoreLines(Arrays.asList(baselineLines), ignoreLines);
            List<String> filteredCollectedLines = filterIgnoreLines(Arrays.asList(collectedLines), ignoreLines);
            
            // 逐行比对
            List<Map<String, Object>> differences = new ArrayList<>();
            int maxLines = Math.max(filteredBaselineLines.size(), filteredCollectedLines.size());
            
            for (int i = 0; i < maxLines; i++) {
                String baselineLine = i < filteredBaselineLines.size() ? filteredBaselineLines.get(i) : "";
                String collectedLine = i < filteredCollectedLines.size() ? filteredCollectedLines.get(i) : "";
                
                if (!baselineLine.equals(collectedLine)) {
                    Map<String, Object> diff = new HashMap<>();
                    diff.put("lineNumber", i + 1);
                    diff.put("baseline", baselineLine);
                    diff.put("collected", collectedLine);
                    diff.put("type", "modified");
                    differences.add(diff);
                }
            }
            
            // 计算差异百分比
            double diffPercentage = maxLines > 0 ? (double) differences.size() / maxLines * 100 : 0;
            
            result.setDiffCount(differences.size());
            result.setDiffPercentage(diffPercentage);
            result.setStatus(differences.isEmpty() ? 1 : 3);
            
            // 将差异详情转换为JSON
            try {
                result.setDiffDetails(objectMapper.writeValueAsString(differences));
            } catch (JsonProcessingException e) {
                result.setDiffDetails("差异详情序列化失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("逐行比对失败", e);
            result.setStatus(2);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    public CompareResult ignoreWhitespaceCompare(String baselineContent, String collectedContent) {
        CompareResult result = new CompareResult();
        
        try {
            // 标准化内容（移除多余空白字符）
            String normalizedBaseline = normalizeWhitespace(baselineContent);
            String normalizedCollected = normalizeWhitespace(collectedContent);
            
            if (normalizedBaseline.equals(normalizedCollected)) {
                result.setStatus(1);
                result.setDiffCount(0);
                result.setDiffPercentage(0.0);
            } else {
                // 计算差异
                int totalChars = Math.max(normalizedBaseline.length(), normalizedCollected.length());
                int diffChars = calculateStringDifference(normalizedBaseline, normalizedCollected);
                double diffPercentage = totalChars > 0 ? (double) diffChars / totalChars * 100 : 0;
                
                result.setStatus(3);
                result.setDiffCount(diffChars);
                result.setDiffPercentage(diffPercentage);
                
                // 生成差异详情
                Map<String, Object> diffDetail = new HashMap<>();
                diffDetail.put("type", "whitespace_normalized");
                diffDetail.put("baseline_length", normalizedBaseline.length());
                diffDetail.put("collected_length", normalizedCollected.length());
                diffDetail.put("difference_chars", diffChars);
                
                try {
                    result.setDiffDetails(objectMapper.writeValueAsString(diffDetail));
                } catch (JsonProcessingException e) {
                    result.setDiffDetails("差异详情序列化失败: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("忽略空白字符比对失败", e);
            result.setStatus(2);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    public CompareResult ignoreCaseCompare(String baselineContent, String collectedContent) {
        CompareResult result = new CompareResult();
        
        try {
            // 转换为小写进行比对
            String lowerBaseline = baselineContent.toLowerCase();
            String lowerCollected = collectedContent.toLowerCase();
            
            if (lowerBaseline.equals(lowerCollected)) {
                result.setStatus(1);
                result.setDiffCount(0);
                result.setDiffPercentage(0.0);
            } else {
                // 计算差异
                int totalChars = Math.max(lowerBaseline.length(), lowerCollected.length());
                int diffChars = calculateStringDifference(lowerBaseline, lowerCollected);
                double diffPercentage = totalChars > 0 ? (double) diffChars / totalChars * 100 : 0;
                
                result.setStatus(3);
                result.setDiffCount(diffChars);
                result.setDiffPercentage(diffPercentage);
                
                // 生成差异详情
                Map<String, Object> diffDetail = new HashMap<>();
                diffDetail.put("type", "case_insensitive");
                diffDetail.put("baseline_length", lowerBaseline.length());
                diffDetail.put("collected_length", lowerCollected.length());
                diffDetail.put("difference_chars", diffChars);
                
                try {
                    result.setDiffDetails(objectMapper.writeValueAsString(diffDetail));
                } catch (JsonProcessingException e) {
                    result.setDiffDetails("差异详情序列化失败: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("忽略大小写比对失败", e);
            result.setStatus(2);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    @Override
    public CompareResult smartDiffCompare(String baselineContent, String collectedContent, String[] ignoreLines) {
        CompareResult result = new CompareResult();
        
        try {
            // 智能比对：结合多种策略
            String[] baselineLines = baselineContent.split("\n");
            String[] collectedLines = collectedContent.split("\n");
            
            // 1. 过滤忽略行
            List<String> filteredBaselineLines = filterIgnoreLines(Arrays.asList(baselineLines), ignoreLines);
            List<String> filteredCollectedLines = filterIgnoreLines(Arrays.asList(collectedLines), ignoreLines);
            
            // 2. 标准化处理（移除空白字符、转换为小写）
            List<String> normalizedBaseline = normalizeLines(filteredBaselineLines);
            List<String> normalizedCollected = normalizeLines(filteredCollectedLines);
            
            // 3. 计算相似度
            double similarity = calculateSimilarity(normalizedBaseline, normalizedCollected);
            double diffPercentage = 100 - similarity;
            
            // 4. 生成详细差异
            List<Map<String, Object>> differences = generateSmartDifferences(
                filteredBaselineLines, filteredCollectedLines, normalizedBaseline, normalizedCollected
            );
            
            result.setDiffCount(differences.size());
            result.setDiffPercentage(diffPercentage);
            result.setStatus(diffPercentage <= 5.0 ? 1 : 3); // 5%以内认为一致
            
            // 将差异详情转换为JSON
            try {
                result.setDiffDetails(objectMapper.writeValueAsString(differences));
            } catch (JsonProcessingException e) {
                result.setDiffDetails("差异详情序列化失败: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("智能比对失败", e);
            result.setStatus(2);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }

    /**
     * 过滤忽略行
     */
    private List<String> filterIgnoreLines(List<String> lines, String[] ignoreKeywords) {
        if (ignoreKeywords == null || ignoreKeywords.length == 0) {
            return lines;
        }
        
        return lines.stream()
            .filter(line -> {
                String lowerLine = line.toLowerCase();
                return Arrays.stream(ignoreKeywords)
                    .noneMatch(keyword -> lowerLine.contains(keyword.toLowerCase().trim()));
            })
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 标准化空白字符
     */
    private String normalizeWhitespace(String content) {
        return content.replaceAll("\\s+", " ").trim();
    }

    /**
     * 标准化行内容
     */
    private List<String> normalizeLines(List<String> lines) {
        return lines.stream()
            .map(this::normalizeWhitespace)
            .map(String::toLowerCase)
            .filter(line -> !line.isEmpty())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 计算字符串差异
     */
    private int calculateStringDifference(String str1, String str2) {
        int maxLength = Math.max(str1.length(), str2.length());
        int minLength = Math.min(str1.length(), str2.length());
        
        int diff = maxLength - minLength;
        for (int i = 0; i < minLength; i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                diff++;
            }
        }
        
        return diff;
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(List<String> list1, List<String> list2) {
        if (list1.isEmpty() && list2.isEmpty()) {
            return 100.0;
        }
        if (list1.isEmpty() || list2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> set1 = new HashSet<>(list1);
        Set<String> set2 = new HashSet<>(list2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size() * 100;
    }

    /**
     * 生成智能差异详情
     */
    private List<Map<String, Object>> generateSmartDifferences(List<String> originalBaseline, 
                                                              List<String> originalCollected,
                                                              List<String> normalizedBaseline, 
                                                              List<String> normalizedCollected) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        // 找出新增、删除、修改的行
        Set<String> baselineSet = new HashSet<>(normalizedBaseline);
        Set<String> collectedSet = new HashSet<>(normalizedCollected);
        
        // 新增的行
        for (int i = 0; i < originalCollected.size(); i++) {
            String normalizedLine = normalizeWhitespace(originalCollected.get(i)).toLowerCase();
            if (!baselineSet.contains(normalizedLine)) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("lineNumber", i + 1);
                diff.put("type", "added");
                diff.put("content", originalCollected.get(i));
                differences.add(diff);
            }
        }
        
        // 删除的行
        for (int i = 0; i < originalBaseline.size(); i++) {
            String normalizedLine = normalizeWhitespace(originalBaseline.get(i)).toLowerCase();
            if (!collectedSet.contains(normalizedLine)) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("lineNumber", i + 1);
                diff.put("type", "deleted");
                diff.put("content", originalBaseline.get(i));
                differences.add(diff);
            }
        }
        
        return differences;
    }
}



