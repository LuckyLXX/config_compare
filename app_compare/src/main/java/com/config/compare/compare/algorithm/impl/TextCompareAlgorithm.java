package com.config.compare.compare.algorithm.impl;

import com.config.compare.compare.algorithm.CompareAlgorithm;
import com.config.compare.compare.model.CompareContext;
import com.config.compare.compare.model.CompareResultModel;
import com.config.compare.compare.model.DiffItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 通用文本比对算法
 * 支持左右对比显示，类似bcompare的效果
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class TextCompareAlgorithm implements CompareAlgorithm {

    @Override
    public String getAlgorithmType() {
        return "TEXT";
    }

    @Override
    public String getAlgorithmName() {
        return "通用文本比对算法";
    }

    @Override
    public CompareResultModel compare(CompareContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("开始执行文本比对，基线长度：{}，当前内容长度：{}", 
                     context.getBaselineContent() != null ? context.getBaselineContent().length() : 0,
                     context.getCurrentContent() != null ? context.getCurrentContent().length() : 0);
            
            // 添加详细的调试日志
            log.info("=== 比对内容详情 ===");
            log.info("基线内容：\n{}", context.getBaselineContent());
            log.info("当前内容：\n{}", context.getCurrentContent());
            log.info("==================");
            
            CompareResultModel result = doTextCompare(context);
            result.setAlgorithmType(getAlgorithmType());
            result.setDuration(startTime);
            
            log.debug("文本比对完成，一致性：{}，差异数量：{}", result.isConsistent(), result.getDiffCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("文本比对过程发生异常", e);
            CompareResultModel result = CompareResultModel.fail("文本比对异常：" + e.getMessage());
            result.setAlgorithmType(getAlgorithmType());
            result.setDuration(startTime);
            return result;
        }
    }

    @Override
    public boolean supports(String contentType) {
        // 支持所有文本类型的内容
        return "TEXT".equalsIgnoreCase(contentType) || 
               "PLAIN".equalsIgnoreCase(contentType) ||
               "CONFIG".equalsIgnoreCase(contentType) ||
               "SSH_TEXT".equalsIgnoreCase(contentType) ||
               "CONFIG_FILE".equalsIgnoreCase(contentType) ||
               "ENV_VAR".equalsIgnoreCase(contentType) ||
               "YAML_CONFIG".equalsIgnoreCase(contentType) ||
               "XML_CONFIG".equalsIgnoreCase(contentType) ||
               "INI_CONFIG".equalsIgnoreCase(contentType);
    }

    @Override
    public String getDescription() {
        return "通用文本差异比对算法，支持所有文本内容的左右对比显示，类似bcompare效果";
    }

    /**
     * 执行文本比对
     */
    private CompareResultModel doTextCompare(CompareContext context) {
        String baseline = context.getBaselineContent();
        String current = context.getCurrentContent();
        
        if (baseline == null && current == null) {
            CompareResultModel result = CompareResultModel.success(true);
            result.setConsistencyScore(java.math.BigDecimal.valueOf(100));
            return result;
        }
        
        if (baseline == null || current == null) {
            CompareResultModel result = CompareResultModel.success(false);
            result.addDiffItem(createContentNullDiff(baseline == null ? "基线内容" : "当前内容"));
            result.calculateConsistencyScore(1);
            return result;
        }
        
        // 获取比对规则
        Map<String, Object> compareRules = context.getCompareRules();
        
        // 按行分割内容
        List<String> baselineLines = splitLines(baseline);
        List<String> currentLines = splitLines(current);
        
        CompareResultModel result = CompareResultModel.success(false);
        
        // 使用改进的行比对算法，支持左右对比
        compareLinesWithSideBySide(baselineLines, currentLines, result, compareRules);
        
        // 计算一致性评分
        int totalLines = Math.max(baselineLines.size(), currentLines.size());
        result.calculateConsistencyScore(totalLines);
        
        // 生成差异摘要
        result.setDiffSummary(generateTextDiffSummary(result, baselineLines.size(), currentLines.size()));
        
        return result;
    }

    /**
     * 支持左右对比的行比对算法
     */
    private void compareLinesWithSideBySide(List<String> baselineLines, List<String> currentLines, 
                                          CompareResultModel result, Map<String, Object> compareRules) {
        
        // 添加调试日志
        log.info("=== 行比对详情 ===");
        log.info("原始基线行数: {}", baselineLines.size());
        log.info("原始当前行数: {}", currentLines.size());
        for (int i = 0; i < baselineLines.size(); i++) {
            log.info("基线行{}: '{}'", i + 1, baselineLines.get(i));
        }
        for (int i = 0; i < currentLines.size(); i++) {
            log.info("当前行{}: '{}'", i + 1, currentLines.get(i));
        }
        
        // 获取比对规则
        boolean ignoreWhitespace = getBooleanRule(compareRules, "ignoreWhitespace", false);
        boolean ignoreCase = getBooleanRule(compareRules, "ignoreCase", false);
        boolean ignoreComments = getBooleanRule(compareRules, "ignoreComments", false);
        List<String> ignoreLines = getStringListRule(compareRules, "ignoreLines", new ArrayList<>());
        
        log.info("比对规则: ignoreWhitespace={}, ignoreCase={}, ignoreComments={}, ignoreLines={}", 
                ignoreWhitespace, ignoreCase, ignoreComments, ignoreLines);
        
        // 预处理行内容
        List<String> processedBaselineLines = preprocessLines(baselineLines, ignoreWhitespace, ignoreCase, ignoreComments, ignoreLines);
        List<String> processedCurrentLines = preprocessLines(currentLines, ignoreWhitespace, ignoreCase, ignoreComments, ignoreLines);
        
        log.info("预处理后基线行数: {}", processedBaselineLines.size());
        log.info("预处理后当前行数: {}", processedCurrentLines.size());
        for (int i = 0; i < processedBaselineLines.size(); i++) {
            log.info("处理后基线行{}: '{}'", i + 1, processedBaselineLines.get(i));
        }
        for (int i = 0; i < processedCurrentLines.size(); i++) {
            log.info("处理后当前行{}: '{}'", i + 1, processedCurrentLines.get(i));
        }
        
        // 使用Myers差分算法进行行比对
        List<DiffLine> diffLines = computeDiffLines(processedBaselineLines, processedCurrentLines);
        
        log.info("计算出的差异行数: {}", diffLines.size());
        for (DiffLine diffLine : diffLines) {
            log.info("差异行: type={}, baselineIndex={}, currentIndex={}, baselineValue='{}', currentValue='{}'", 
                    diffLine.type, diffLine.baselineIndex, diffLine.currentIndex, diffLine.baselineValue, diffLine.currentValue);
        }
        
        // 生成差异项
        generateDiffItems(diffLines, baselineLines, currentLines, result);
        
        log.info("生成的差异项数: {}", result.getDiffCount());
        log.info("==================");
    }

    /**
     * 预处理行内容
     */
    private List<String> preprocessLines(List<String> lines, boolean ignoreWhitespace, boolean ignoreCase, 
                                       boolean ignoreComments, List<String> ignoreLines) {
        List<String> processed = new ArrayList<>();
        
        log.info("开始预处理，输入行数: {}", lines.size());
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String processedLine = line;
            
            log.info("处理第{}行: '{}'", i + 1, line);
            
            // 忽略注释行
            if (ignoreComments && isCommentLine(line)) {
                log.info("第{}行被识别为注释行，跳过", i + 1);
                continue;
            }
            
            // 忽略指定关键词的行
            if (shouldIgnoreLine(line, ignoreLines)) {
                log.info("第{}行包含忽略关键词，跳过", i + 1);
                continue;
            }
            
            // 忽略空白字符
            if (ignoreWhitespace) {
                processedLine = processedLine.trim();
                log.info("第{}行去除空白后: '{}'", i + 1, processedLine);
            }
            
            // 忽略大小写
            if (ignoreCase) {
                processedLine = processedLine.toLowerCase();
                log.info("第{}行转小写后: '{}'", i + 1, processedLine);
            }
            
            log.info("第{}行处理完成，添加到结果: '{}'", i + 1, processedLine);
            processed.add(processedLine);
        }
        
        log.info("预处理完成，输出行数: {}", processed.size());
        return processed;
    }

    /**
     * 判断是否为注释行
     */
    private boolean isCommentLine(String line) {
        String trimmed = line.trim();
        return trimmed.startsWith("#") || trimmed.startsWith("//") || trimmed.startsWith("/*") || 
               trimmed.startsWith("*") || trimmed.startsWith("<!--");
    }

    /**
     * 判断是否应该忽略该行
     */
    private boolean shouldIgnoreLine(String line, List<String> ignoreLines) {
        if (ignoreLines.isEmpty()) {
            return false;
        }
        
        String lowerLine = line.toLowerCase();
        for (String ignorePattern : ignoreLines) {
            String trimmedPattern = ignorePattern.toLowerCase().trim();
            if (!trimmedPattern.isEmpty() && lowerLine.contains(trimmedPattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算行差异（行号对齐方式）
     */
    private List<DiffLine> computeDiffLines(List<String> baselineLines, List<String> currentLines) {
        List<DiffLine> diffLines = new ArrayList<>();
        
        // 使用行号对齐的方式，总行数取较大的
        int maxLines = Math.max(baselineLines.size(), currentLines.size());
        
        for (int i = 0; i < maxLines; i++) {
            String baselineLine = i < baselineLines.size() ? baselineLines.get(i) : null;
            String currentLine = i < currentLines.size() ? currentLines.get(i) : null;
            
            if (baselineLine == null && currentLine == null) {
                // 两行都为空，不应该发生
                continue;
            } else if (baselineLine == null) {
                // 基线没有，当前有 - 新增
                diffLines.add(new DiffLine(DiffType.ADD, -1, i, null, currentLine));
            } else if (currentLine == null) {
                // 基线有，当前没有 - 删除
                diffLines.add(new DiffLine(DiffType.DELETE, i, -1, baselineLine, null));
            } else if (!baselineLine.equals(currentLine)) {
                // 两行都有但内容不同 - 修改
                diffLines.add(new DiffLine(DiffType.MODIFY, i, i, baselineLine, currentLine));
            } else {
                // 两行都有且内容相同 - 相同
                diffLines.add(new DiffLine(DiffType.EQUAL, i, i, baselineLine, currentLine));
            }
        }
        
        return diffLines;
    }

    /**
     * 计算LCS矩阵
     */
    private int[][] computeLCS(List<String> baselineLines, List<String> currentLines) {
        int m = baselineLines.size();
        int n = currentLines.size();
        int[][] lcs = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    lcs[i][j] = 0;
                } else if (baselineLines.get(i - 1).equals(currentLines.get(j - 1))) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }
        
        return lcs;
    }

    /**
     * 回溯LCS结果
     */
    private List<DiffLine> backtrackLCS(int[][] lcs, List<String> baselineLines, List<String> currentLines, 
                                       int i, int j) {
        List<DiffLine> result = new ArrayList<>();
        
        if (i == 0 || j == 0) {
            return result;
        }
        
        if (baselineLines.get(i - 1).equals(currentLines.get(j - 1))) {
            result.addAll(backtrackLCS(lcs, baselineLines, currentLines, i - 1, j - 1));
            result.add(new DiffLine(DiffType.EQUAL, i - 1, j - 1, baselineLines.get(i - 1), currentLines.get(j - 1)));
        } else if (lcs[i - 1][j] >= lcs[i][j - 1]) {
            result.addAll(backtrackLCS(lcs, baselineLines, currentLines, i - 1, j));
        } else {
            result.addAll(backtrackLCS(lcs, baselineLines, currentLines, i, j - 1));
        }
        
        return result;
    }

        /**
     * 生成差异项
     */
    private void generateDiffItems(List<DiffLine> diffLines, List<String> baselineLines, 
                                  List<String> currentLines, CompareResultModel result) {
        
        for (DiffLine diffLine : diffLines) {
            switch (diffLine.type) {
                case ADD:
                    // 基线没有，当前有 - 新增
                    DiffItem addItem = DiffItem.createAdd("第" + (diffLine.currentIndex + 1) + "行", diffLine.currentValue);
                    addItem.setDiffLevel(getDiffLevel(diffLine.currentValue));
                    addItem.setDiffCategory("配置新增");
                    addItem.setSuggestAction("确认新增配置是否正确");
                    addItem.setDiffPath("line_" + (diffLine.currentIndex + 1));
                    result.addDiffItem(addItem);
                    break;
                    
                case DELETE:
                    // 基线有，当前没有 - 缺失
                    DiffItem deleteItem = DiffItem.createDelete("第" + (diffLine.baselineIndex + 1) + "行", diffLine.baselineValue);
                    deleteItem.setDiffLevel(getDiffLevel(diffLine.baselineValue));
                    deleteItem.setDiffCategory("配置缺失");
                    deleteItem.setSuggestAction("确认是否需要保留该配置");
                    deleteItem.setDiffPath("line_" + (diffLine.baselineIndex + 1));
                    result.addDiffItem(deleteItem);
                    break;
                    
                case MODIFY:
                    // 两行都有但内容不同 - 修改
                    DiffItem modifyItem = DiffItem.createModify("第" + (diffLine.baselineIndex + 1) + "行", diffLine.baselineValue, diffLine.currentValue);
                    modifyItem.setDiffLevel(getDiffLevel(diffLine.baselineValue));
                    modifyItem.setDiffCategory("配置修改");
                    modifyItem.setSuggestAction("确认配置修改是否正确");
                    modifyItem.setDiffPath("line_" + (diffLine.baselineIndex + 1));
                    result.addDiffItem(modifyItem);
                    break;
                    
                case EQUAL:
                    // 相同的行，不添加差异项
                    break;
            }
        }
    }

    /**
     * 分割文本行
     */
    private List<String> splitLines(String content) {
        if (!StringUtils.hasText(content)) {
            return new ArrayList<>();
        }
        
        return Arrays.asList(content.split("\r?\n"));
    }

    /**
     * 判断差异级别
     */
    private String getDiffLevel(String line) {
        if (!StringUtils.hasText(line)) {
            return "LOW";
        }
        
        String trimmedLine = line.trim();
        
        // 空行或注释行为低级别
        if (trimmedLine.isEmpty() || isCommentLine(line)) {
            return "LOW";
        }
        
        // 包含关键词的为高级别
        String lowerLine = trimmedLine.toLowerCase();
        if (lowerLine.contains("password") || lowerLine.contains("secret") || 
            lowerLine.contains("token") || lowerLine.contains("key") ||
            lowerLine.contains("port") || lowerLine.contains("host") ||
            lowerLine.contains("database") || lowerLine.contains("redis")) {
            return "HIGH";
        }
        
        // 其他为中级别
        return "MEDIUM";
    }

    /**
     * 创建内容为空的差异项
     */
    private DiffItem createContentNullDiff(String nullContent) {
        DiffItem diffItem = new DiffItem();
        diffItem.setDiffType("MODIFY");
        diffItem.setDiffKey("内容完整性");
        diffItem.setDiffLevel("HIGH");
        diffItem.setDiffCategory("内容缺失");
        diffItem.setDescription(nullContent + "为空");
        diffItem.setSuggestAction("检查配置采集是否正常");
        return diffItem;
    }

    /**
     * 生成文本差异摘要
     */
    private String generateTextDiffSummary(CompareResultModel result, int baselineLines, int currentLines) {
        StringBuilder summary = new StringBuilder();
        summary.append("文本比对结果：");
        summary.append("基线行数=").append(baselineLines);
        summary.append("，当前行数=").append(currentLines);
        summary.append("，总差异=").append(result.getDiffCount());
        
        if (result.getHighDiffCount() > 0) {
            summary.append("，高级别差异=").append(result.getHighDiffCount());
        }
        if (result.getMediumDiffCount() > 0) {
            summary.append("，中级别差异=").append(result.getMediumDiffCount());
        }
        if (result.getLowDiffCount() > 0) {
            summary.append("，低级别差异=").append(result.getLowDiffCount());
        }
        
        summary.append("，一致性评分=").append(result.getConsistencyScore()).append("%");
        
        return summary.toString();
    }

    /**
     * 获取布尔类型的规则值
     */
    private boolean getBooleanRule(Map<String, Object> rules, String key, boolean defaultValue) {
        if (rules == null || !rules.containsKey(key)) {
            return defaultValue;
        }
        Object value = rules.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }

    /**
     * 获取字符串列表类型的规则值
     */
    private List<String> getStringListRule(Map<String, Object> rules, String key, List<String> defaultValue) {
        if (rules == null || !rules.containsKey(key)) {
            return defaultValue;
        }
        Object value = rules.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.trim().isEmpty()) {
                return defaultValue; // 空字符串返回默认值
            }
            if (str.contains(",")) {
                return Arrays.asList(str.split(","));
            } else {
                return Arrays.asList(str);
            }
        }
        return defaultValue;
    }

    /**
     * 差异行类型枚举
     */
    private enum DiffType {
        ADD, DELETE, MODIFY, EQUAL
    }

    /**
     * 差异行模型
     */
    private static class DiffLine {
        DiffType type;
        int baselineIndex;
        int currentIndex;
        String baselineValue;
        String currentValue;

        DiffLine(DiffType type, int baselineIndex, int currentIndex, String baselineValue, String currentValue) {
            this.type = type;
            this.baselineIndex = baselineIndex;
            this.currentIndex = currentIndex;
            this.baselineValue = baselineValue;
            this.currentValue = currentValue;
        }
    }
}