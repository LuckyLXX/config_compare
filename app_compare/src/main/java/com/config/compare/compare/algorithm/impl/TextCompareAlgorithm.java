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
        
        // 获取比对规则
        boolean ignoreWhitespace = getBooleanRule(compareRules, "ignoreWhitespace", false);
        boolean ignoreCase = getBooleanRule(compareRules, "ignoreCase", false);
        boolean ignoreComments = getBooleanRule(compareRules, "ignoreComments", false);
        List<String> ignoreLines = getStringListRule(compareRules, "ignoreLines", new ArrayList<>());
        
        // 预处理行内容
        List<String> processedBaselineLines = preprocessLines(baselineLines, ignoreWhitespace, ignoreCase, ignoreComments, ignoreLines);
        List<String> processedCurrentLines = preprocessLines(currentLines, ignoreWhitespace, ignoreCase, ignoreComments, ignoreLines);
        
        // 使用Myers差分算法进行行比对
        List<DiffLine> diffLines = computeDiffLines(processedBaselineLines, processedCurrentLines);
        
        // 生成差异项
        generateDiffItems(diffLines, baselineLines, currentLines, result);
    }

    /**
     * 预处理行内容
     */
    private List<String> preprocessLines(List<String> lines, boolean ignoreWhitespace, boolean ignoreCase, 
                                       boolean ignoreComments, List<String> ignoreLines) {
        List<String> processed = new ArrayList<>();
        
        for (String line : lines) {
            String processedLine = line;
            
            // 忽略注释行
            if (ignoreComments && isCommentLine(line)) {
                continue;
            }
            
            // 忽略指定关键词的行
            if (shouldIgnoreLine(line, ignoreLines)) {
                continue;
            }
            
            // 忽略空白字符
            if (ignoreWhitespace) {
                processedLine = processedLine.trim();
            }
            
            // 忽略大小写
            if (ignoreCase) {
                processedLine = processedLine.toLowerCase();
            }
            
            processed.add(processedLine);
        }
        
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
            if (lowerLine.contains(ignorePattern.toLowerCase().trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算行差异
     */
    private List<DiffLine> computeDiffLines(List<String> baselineLines, List<String> currentLines) {
        List<DiffLine> diffLines = new ArrayList<>();
        
        // 使用简单的LCS算法计算差异
        int[][] lcs = computeLCS(baselineLines, currentLines);
        List<DiffLine> lcsResult = backtrackLCS(lcs, baselineLines, currentLines, baselineLines.size(), currentLines.size());
        
        // 转换为完整的差异行列表
        int baselineIndex = 0;
        int currentIndex = 0;
        
        for (DiffLine diffLine : lcsResult) {
            // 添加删除的行
            while (baselineIndex < diffLine.baselineIndex) {
                diffLines.add(new DiffLine(DiffType.DELETE, baselineIndex, -1, baselineLines.get(baselineIndex), null));
                baselineIndex++;
            }
            
            // 添加新增的行
            while (currentIndex < diffLine.currentIndex) {
                diffLines.add(new DiffLine(DiffType.ADD, -1, currentIndex, null, currentLines.get(currentIndex)));
                currentIndex++;
            }
            
            // 添加相同的行
            if (diffLine.type == DiffType.EQUAL) {
                diffLines.add(diffLine);
                baselineIndex++;
                currentIndex++;
            }
        }
        
        // 添加剩余的行
        while (baselineIndex < baselineLines.size()) {
            diffLines.add(new DiffLine(DiffType.DELETE, baselineIndex, -1, baselineLines.get(baselineIndex), null));
            baselineIndex++;
        }
        
        while (currentIndex < currentLines.size()) {
            diffLines.add(new DiffLine(DiffType.ADD, -1, currentIndex, null, currentLines.get(currentIndex)));
            currentIndex++;
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
                    DiffItem addItem = DiffItem.createAdd("第" + (diffLine.currentIndex + 1) + "行", diffLine.currentValue);
                    addItem.setDiffLevel(getDiffLevel(diffLine.currentValue));
                    addItem.setDiffCategory("行新增");
                    addItem.setSuggestAction("确认新增配置是否正确");
                    addItem.setDiffPath("line_" + (diffLine.currentIndex + 1));
                    result.addDiffItem(addItem);
                    break;
                    
                case DELETE:
                    DiffItem deleteItem = DiffItem.createDelete("第" + (diffLine.baselineIndex + 1) + "行", diffLine.baselineValue);
                    deleteItem.setDiffLevel(getDiffLevel(diffLine.baselineValue));
                    deleteItem.setDiffCategory("行删除");
                    deleteItem.setSuggestAction("确认是否需要保留该配置");
                    deleteItem.setDiffPath("line_" + (diffLine.baselineIndex + 1));
                    result.addDiffItem(deleteItem);
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
        ADD, DELETE, EQUAL
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