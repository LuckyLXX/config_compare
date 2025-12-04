package com.config.compare.compare.algorithm.impl;

import com.config.compare.compare.algorithm.CompareAlgorithm;
import com.config.compare.compare.model.AlignedLine;
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
            log.info("基线内容长度：{}", context.getBaselineContent() != null ? context.getBaselineContent().length() : 0);
            log.info("当前内容长度：{}", context.getCurrentContent() != null ? context.getCurrentContent().length() : 0);
            
            // 检查是否为XML内容
            boolean isXmlBaseline = isXmlContent(context.getBaselineContent());
            boolean isXmlCurrent = isXmlContent(context.getCurrentContent());
            log.info("基线是否为XML：{}", isXmlBaseline);
            log.info("当前是否为XML：{}", isXmlCurrent);
            
            // 只记录前500字符避免日志过长
            String baselinePreview = context.getBaselineContent() != null && context.getBaselineContent().length() > 500 ?
                context.getBaselineContent().substring(0, 500) + "..." : context.getBaselineContent();
            String currentPreview = context.getCurrentContent() != null && context.getCurrentContent().length() > 500 ?
                context.getCurrentContent().substring(0, 500) + "..." : context.getCurrentContent();
            
            log.info("基线内容前500字符：\n{}", baselinePreview);
            log.info("当前内容前500字符：\n{}", currentPreview);
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
        
        // 检测并规范化JSON内容
        boolean isJson = isJsonContent(baseline) && isJsonContent(current);
        if (isJson) {
            log.info("🔍 检测到JSON内容，进行格式规范化");
            baseline = normalizeJsonFormat(baseline);
            current = normalizeJsonFormat(current);
        }
        
        // 按行分割内容
        List<String> baselineLines = splitLines(baseline);
        List<String> currentLines = splitLines(current);
        
        log.info("🔍 行分割结果：基线行数={}，当前行数={}", baselineLines.size(), currentLines.size());
        log.info("🔍 基线前10行：");
        for (int i = 0; i < Math.min(10, baselineLines.size()); i++) {
            log.info("  基线行{}: '{}'", i + 1, baselineLines.get(i));
        }
        log.info("🔍 当前前10行：");
        for (int i = 0; i < Math.min(10, currentLines.size()); i++) {
            log.info("  当前行{}: '{}'", i + 1, currentLines.get(i));
        }
        
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
        List<MyersDiffAlgorithm.DiffOp> diffOps = MyersDiffAlgorithm.computeDiff(processedBaselineLines, processedCurrentLines);
        List<DiffLine> diffLines = convertMyersDiffToDiffLines(diffOps, processedBaselineLines, processedCurrentLines);
        
        log.info("计算出的差异行数: {}", diffLines.size());
        for (DiffLine diffLine : diffLines) {
            log.info("差异行: type={}, baselineIndex={}, currentIndex={}, baselineValue='{}', currentValue='{}'", 
                    diffLine.type, diffLine.baselineIndex, diffLine.currentIndex, diffLine.baselineValue, diffLine.currentValue);
        }
        
        // 【新增】生成完整的对齐行信息供前端使用
        generateAlignedLines(diffLines, result);
        
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
     * 计算行差异（使用LCS算法进行精确比对）
     */
    private List<DiffLine> computeDiffLines(List<String> baselineLines, List<String> currentLines) {
        // 检查是否为XML内容，如果是则使用专门的XML比对算法
        boolean isXmlBaseline = isXmlContent(baselineLines);
        boolean isXmlCurrent = isXmlContent(currentLines);
        
        log.info("🔍 内容类型检测：基线是XML={}，当前是XML={}", isXmlBaseline, isXmlCurrent);
        
        List<DiffLine> diffLines;
        
        if (isXmlBaseline || isXmlCurrent) {
            // 对于XML内容，使用专门的XML比对算法
            log.info("🔍 使用XML专用比对算法");
            diffLines = computeXmlDiffLines(baselineLines, currentLines);
        } else {
            // 对于普通文本，使用标准LCS算法
            log.info("🔍 使用标准文本比对算法");
            int[][] lcs = computeLCS(baselineLines, currentLines);
            diffLines = backtrackLCS(lcs, baselineLines, currentLines,
                                         baselineLines.size(), currentLines.size());
            
            // 反转结果，因为回溯是从后往前的
            Collections.reverse(diffLines);
            
            // 为差异行添加准确的行号映射
            diffLines = enhanceDiffLinesWithLineNumbers(diffLines, baselineLines, currentLines);
        }
        
        return diffLines;
    }
    
    /**
     * 检查是否为XML内容（重载方法）
     */
    private boolean isXmlContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = content.trim();
        // 检查XML声明或XML标签
        return trimmed.startsWith("<?xml") ||
               (trimmed.startsWith("<") && trimmed.contains(">")) ||
               trimmed.contains("</") ||
               trimmed.contains("<xml");
    }
    
    /**
     * 检查是否为XML内容
     */
    private boolean isXmlContent(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        
        // 查找第一个非空行
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return trimmed.startsWith("<?xml") ||
                       (trimmed.startsWith("<") && trimmed.contains(">"));
            }
        }
        return false;
    }
    
    /**
     * 计算XML文件的行差异（优化版本）
     */
    private List<DiffLine> computeXmlDiffLines(List<String> baselineLines, List<String> currentLines) {
        List<DiffLine> diffLines = new ArrayList<>();
        int baselineIndex = 0;
        int currentIndex = 0;
        
        // 使用改进的LCS算法，但保留原始行号信息
        int[][] lcs = computeLCS(baselineLines, currentLines);
        List<DiffLine> lcsDiffLines = backtrackLCS(lcs, baselineLines, currentLines,
                                                  baselineLines.size(), currentLines.size());
        Collections.reverse(lcsDiffLines);
        
        // 处理LCS结果，确保行号映射正确
        for (DiffLine diffLine : lcsDiffLines) {
            switch (diffLine.type) {
                case EQUAL:
                    diffLines.add(new DiffLine(DiffType.EQUAL, baselineIndex, currentIndex,
                                              baselineLines.get(baselineIndex), currentLines.get(currentIndex)));
                    baselineIndex++;
                    currentIndex++;
                    break;
                    
                case MODIFY:
                    diffLines.add(new DiffLine(DiffType.MODIFY, baselineIndex, currentIndex,
                                              baselineLines.get(baselineIndex), currentLines.get(currentIndex)));
                    baselineIndex++;
                    currentIndex++;
                    break;
                    
                case DELETE:
                    diffLines.add(new DiffLine(DiffType.DELETE, baselineIndex, currentIndex,
                                              baselineLines.get(baselineIndex), ""));
                    baselineIndex++;
                    break;
                    
                case ADD:
                    diffLines.add(new DiffLine(DiffType.ADD, baselineIndex, currentIndex,
                                              "", currentLines.get(currentIndex)));
                    currentIndex++;
                    break;
            }
        }
        
        // 处理剩余的行
        while (baselineIndex < baselineLines.size()) {
            diffLines.add(new DiffLine(DiffType.DELETE, baselineIndex, currentIndex,
                                      baselineLines.get(baselineIndex), ""));
            baselineIndex++;
        }
        
        while (currentIndex < currentLines.size()) {
            diffLines.add(new DiffLine(DiffType.ADD, baselineIndex, currentIndex,
                                      "", currentLines.get(currentIndex)));
            currentIndex++;
        }
        
        return diffLines;
    }
    
    /**
     * 增强差异行信息，添加准确的行号映射
     */
    private List<DiffLine> enhanceDiffLinesWithLineNumbers(List<DiffLine> diffLines,
                                                          List<String> baselineLines, List<String> currentLines) {
        List<DiffLine> enhancedLines = new ArrayList<>();
        int baselineIndex = 0;
        int currentIndex = 0;
        
        for (DiffLine diffLine : diffLines) {
            switch (diffLine.type) {
                case EQUAL:
                    // 相同行，直接使用原始索引
                    enhancedLines.add(new DiffLine(DiffType.EQUAL, baselineIndex, currentIndex,
                                                diffLine.baselineValue, diffLine.currentValue));
                    baselineIndex++;
                    currentIndex++;
                    break;
                    
                case MODIFY:
                    // 修改行，使用当前索引
                    enhancedLines.add(new DiffLine(DiffType.MODIFY, baselineIndex, currentIndex,
                                                diffLine.baselineValue, diffLine.currentValue));
                    baselineIndex++;
                    currentIndex++;
                    break;
                    
                case DELETE:
                    // 删除行，基线有当前没有
                    enhancedLines.add(new DiffLine(DiffType.DELETE, baselineIndex, -1,
                                                diffLine.baselineValue, null));
                    baselineIndex++;
                    break;
                    
                case ADD:
                    // 新增行，当前有基线没有
                    enhancedLines.add(new DiffLine(DiffType.ADD, -1, currentIndex,
                                                null, diffLine.currentValue));
                    currentIndex++;
                    break;
            }
        }
        
        // 处理剩余的行
        while (baselineIndex < baselineLines.size()) {
            enhancedLines.add(new DiffLine(DiffType.DELETE, baselineIndex, -1,
                                        baselineLines.get(baselineIndex), null));
            baselineIndex++;
        }
        
        while (currentIndex < currentLines.size()) {
            enhancedLines.add(new DiffLine(DiffType.ADD, -1, currentIndex,
                                        null, currentLines.get(currentIndex)));
            currentIndex++;
        }
        
        return enhancedLines;
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
     * 将Myers算法的DiffOp转换为DiffLine，并优化相邻的删除+插入为修改
     */
    private List<DiffLine> convertMyersDiffToDiffLines(List<MyersDiffAlgorithm.DiffOp> diffOps, 
                                                      List<String> baselineLines, 
                                                      List<String> currentLines) {
        List<DiffLine> diffLines = new ArrayList<>();
        int baselineIndex = 0;
        int currentIndex = 0;

        for (int i = 0; i < diffOps.size(); i++) {
            MyersDiffAlgorithm.DiffOp diffOp = diffOps.get(i);
            
            switch (diffOp.getOperation()) {
                case EQUAL:
                    diffLines.add(new DiffLine(DiffType.EQUAL, baselineIndex, currentIndex,
                                             diffOp.getText(), diffOp.getText()));
                    baselineIndex++;
                    currentIndex++;
                    break;

                case DELETE:
                    // 检查下一个操作是否是INSERT，如果是则考虑合并为MODIFY
                    if (i + 1 < diffOps.size() && 
                        diffOps.get(i + 1).getOperation() == MyersDiffAlgorithm.DiffOperation.INSERT) {
                        
                        MyersDiffAlgorithm.DiffOp nextOp = diffOps.get(i + 1);
                        
                        // 【修复】判断是否应该合并为MODIFY
                        // 只有当DELETE和INSERT都有实质内容时才合并
                        boolean deleteHasContent = diffOp.getText() != null && !diffOp.getText().trim().isEmpty();
                        boolean insertHasContent = nextOp.getText() != null && !nextOp.getText().trim().isEmpty();
                        
                        if (deleteHasContent && insertHasContent) {
                            // 两者都有实质内容，合并为MODIFY
                            diffLines.add(new DiffLine(DiffType.MODIFY, baselineIndex, currentIndex,
                                                     diffOp.getText(), nextOp.getText()));
                            baselineIndex++;
                            currentIndex++;
                            i++; // 跳过下一个INSERT操作
                        } else if (deleteHasContent && !insertHasContent) {
                            // DELETE有内容，INSERT只是空白 -> 这是删除
                            diffLines.add(new DiffLine(DiffType.DELETE, baselineIndex, -1,
                                                     diffOp.getText(), ""));
                            baselineIndex++;
                            // 跳过空白的INSERT
                            i++;
                        } else if (!deleteHasContent && insertHasContent) {
                            // DELETE只是空白，INSERT有内容 -> 这是新增
                            diffLines.add(new DiffLine(DiffType.ADD, -1, currentIndex,
                                                     "", nextOp.getText()));
                            currentIndex++;
                            // 跳过空白的DELETE，不增加baselineIndex
                            i++;
                        } else {
                            // 两者都是空白，当作相同处理（不生成差异）
                            baselineIndex++;
                            currentIndex++;
                            i++;
                        }
                    } else {
                        diffLines.add(new DiffLine(DiffType.DELETE, baselineIndex, -1,
                                                 diffOp.getText(), ""));
                        baselineIndex++;
                    }
                    break;

                case INSERT:
                    // 如果前一个不是DELETE（已经被处理），则这是纯插入
                    diffLines.add(new DiffLine(DiffType.ADD, -1, currentIndex,
                                             "", diffOp.getText()));
                    currentIndex++;
                    break;
            }
        }

        return diffLines;
    }

    /**
     * 回溯LCS结果 (已弃用，使用Myers算法替代)
     */
    @Deprecated
    private List<DiffLine> backtrackLCS(int[][] lcs, List<String> baselineLines, List<String> currentLines, 
                                       int i, int j) {
        List<DiffLine> result = new ArrayList<>();
        
        // 处理边界情况
        while (i > 0 && j > 0) {
            if (baselineLines.get(i - 1).equals(currentLines.get(j - 1))) {
                // 相同行
                result.add(new DiffLine(DiffType.EQUAL, i - 1, j - 1, baselineLines.get(i - 1), currentLines.get(j - 1)));
                i--;
                j--;
            } else if (lcs[i - 1][j] >= lcs[i][j - 1]) {
                // 删除行（基线有，当前没有）
                result.add(new DiffLine(DiffType.DELETE, i - 1, -1, baselineLines.get(i - 1), ""));
                i--;
            } else {
                // 新增行（当前有，基线没有）
                result.add(new DiffLine(DiffType.ADD, -1, j - 1, "", currentLines.get(j - 1)));
                j--;
            }
        }
        
        // 处理剩余的基线行（都是删除）
        while (i > 0) {
            result.add(new DiffLine(DiffType.DELETE, i - 1, -1, baselineLines.get(i - 1), ""));
            i--;
        }
        
        // 处理剩余的当前行（都是新增）
        while (j > 0) {
            result.add(new DiffLine(DiffType.ADD, -1, j - 1, "", currentLines.get(j - 1)));
            j--;
        }
        
        return result;
    }

    /**
     * 生成完整的对齐行信息（供前端使用）
     */
    private void generateAlignedLines(List<DiffLine> diffLines, CompareResultModel result) {
        List<AlignedLine> alignedLines = new ArrayList<>();
        
        int baselineLineNumber = 1;
        int currentLineNumber = 1;
        
        for (DiffLine diffLine : diffLines) {
            AlignedLine alignedLine = new AlignedLine();
            
            switch (diffLine.type) {
                case EQUAL:
                case MODIFY:
                    // 两边都有内容
                    alignedLine.setBaselineLineNumber(baselineLineNumber++);
                    alignedLine.setCurrentLineNumber(currentLineNumber++);
                    alignedLine.setBaselineContent(diffLine.baselineValue);
                    alignedLine.setCurrentContent(diffLine.currentValue);
                    alignedLine.setDiffType(diffLine.type.name());
                    break;
                    
                case DELETE:
                    // 基线有，当前没有
                    alignedLine.setBaselineLineNumber(baselineLineNumber++);
                    alignedLine.setCurrentLineNumber(-1); // -1表示空行
                    alignedLine.setBaselineContent(diffLine.baselineValue);
                    alignedLine.setCurrentContent("");
                    alignedLine.setDiffType("DELETE");
                    break;
                    
                case ADD:
                    // 基线没有，当前有
                    alignedLine.setBaselineLineNumber(-1); // -1表示空行
                    alignedLine.setCurrentLineNumber(currentLineNumber++);
                    alignedLine.setBaselineContent("");
                    alignedLine.setCurrentContent(diffLine.currentValue);
                    alignedLine.setDiffType("ADD");
                    break;
            }
            
            alignedLines.add(alignedLine);
        }
        
        result.setAlignedLines(alignedLines);
        log.info("生成完整对齐行信息: {} 行", alignedLines.size());
    }

        /**
     * 生成差异项（增强版 - 支持行内差异检测）
     */
    private void generateDiffItems(List<DiffLine> diffLines, List<String> baselineLines,
                                  List<String> currentLines, CompareResultModel result) {

        // 检查是否为XML文件
        boolean isXmlFile = isXmlContent(baselineLines) || isXmlContent(currentLines);
        
        int baselineLineCounter = 1;
        int currentLineCounter = 1;

        for (DiffLine diffLine : diffLines) {
            switch (diffLine.type) {
                case ADD:
                    // 基线没有，当前有 - 新增
                    String addKey = "新增第" + currentLineCounter + "行";
                    DiffItem addItem = DiffItem.createAdd(addKey, diffLine.currentValue);
                    addItem.setDiffLevel(getDiffLevel(diffLine.currentValue));
                    addItem.setDiffCategory("配置新增");
                    addItem.setSuggestAction("确认新增配置是否正确");
                    addItem.setDiffPath("line_" + currentLineCounter);
                    addItem.setBaselineValue("");
                    addItem.setCurrentValue(diffLine.currentValue);
                    
                    // 设置行号信息
                    // addItem.setLineNumbers(null, currentLineCounter);
                    
                    // 添加行内差异分析
                    // InlineDiffResult inlineDiff = analyzeInlineDiffs("", diffLine.currentValue);
                    // addItem.setInlineDiff(inlineDiff);

                    result.addDiffItem(addItem);
                    currentLineCounter++;
                    break;

                case DELETE:
                    // 基线有，当前没有 - 配置删除
                    String deleteKey = "删除第" + baselineLineCounter + "行";
                    DiffItem deleteItem = DiffItem.createDelete(deleteKey, diffLine.baselineValue);
                    deleteItem.setDiffLevel(getDiffLevel(diffLine.baselineValue));
                    deleteItem.setDiffCategory("配置删除");
                    deleteItem.setSuggestAction("确认删除配置是否正确");
                    deleteItem.setDiffPath("line_" + baselineLineCounter);
                    deleteItem.setBaselineValue(diffLine.baselineValue);
                    deleteItem.setCurrentValue("");
                    
                    // 设置行号信息
                    // deleteItem.setLineNumbers(baselineLineCounter, null);
                    
                    // 添加行内差异分析
                    // InlineDiffResult inlineDiff2 = analyzeInlineDiffs(diffLine.baselineValue, "");
                    // deleteItem.setInlineDiff(inlineDiff2);

                    result.addDiffItem(deleteItem);
                    baselineLineCounter++;
                    break;

                case MODIFY:
                    // 两行都有但内容不同 - 修改
                    String modifyKey = "修改第" + baselineLineCounter + "行";
                    DiffItem modifyItem = DiffItem.createModify(modifyKey, diffLine.baselineValue, diffLine.currentValue);
                    modifyItem.setDiffLevel(getDiffLevel(diffLine.baselineValue));
                    modifyItem.setDiffCategory("配置修改");
                    modifyItem.setSuggestAction("确认配置修改是否正确");
                    modifyItem.setDiffPath("line_" + baselineLineCounter);
                    modifyItem.setBaselineValue(diffLine.baselineValue);
                    modifyItem.setCurrentValue(diffLine.currentValue);
                    
                    // 设置行号信息
                    // modifyItem.setLineNumbers(baselineLineCounter, currentLineCounter);
                    
                    // 添加行内差异分析
                    // InlineDiffResult inlineDiff3 = analyzeInlineDiffs(diffLine.baselineValue, diffLine.currentValue);
                    // modifyItem.setInlineDiff(inlineDiff3);

                    result.addDiffItem(modifyItem);
                    baselineLineCounter++;
                    currentLineCounter++;
                    break;

                case EQUAL:
                    // 相同的行，不添加差异项，但需要更新行号
                    baselineLineCounter++;
                    currentLineCounter++;
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

        String[] rawLines = content.split("\\r?\\n", -1);
        List<String> lines = new ArrayList<>(rawLines.length);
        for (String rawLine : rawLines) {
            if (rawLine == null) {
                lines.add("");
            } else if (rawLine.endsWith("\r")) {
                lines.add(rawLine.substring(0, rawLine.length() - 1));
            } else {
                lines.add(rawLine);
            }
        }

        // 去掉结尾多余的空行，避免仅因额外换行出现差异
        while (!lines.isEmpty() && !StringUtils.hasText(lines.get(lines.size() - 1))) {
            lines.remove(lines.size() - 1);
        }
        return lines;
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
    @SuppressWarnings("unchecked")
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
     * 行内差异分析 - 检测一行内的具体差异
     */
    private InlineDiffResult analyzeInlineDiffs(String baselineLine, String currentLine) {
        if (baselineLine == null && currentLine == null) {
            return new InlineDiffResult("", "", java.util.Collections.emptyList());
        }

        if (baselineLine == null) {
            baselineLine = "";
        }
        if (currentLine == null) {
            currentLine = "";
        }

        // 使用Myers算法进行字符级差异分析
        InlineDiffResult inlineDiff = computeInlineDiff(baselineLine, currentLine);

        log.debug("行内差异分析完成：基线='{}'，当前='{}'，差异段数={}",
                 baselineLine, currentLine, inlineDiff.getDiffSegments().size());
        
        return inlineDiff;
    }

    /**
     * 计算行内差异（优化版）
     */
    private InlineDiffResult computeInlineDiff(String baseline, String current) {
        List<InlineDiffSegment> segments = new ArrayList<>();

        if (baseline.equals(current)) {
            // 完全相同，返回一个相等段
            segments.add(new InlineDiffSegment(InlineDiffType.EQUAL, baseline, current, 0, baseline.length(), 0, current.length()));
            return new InlineDiffResult(baseline, current, segments);
        }

        // 使用改进的Myers算法计算字符级差异
        List<CharDiffOp> diffOps = computeCharacterLevelDiff(baseline, current);
        
        // 将diff操作转换为段
        convertDiffOpsToSegments(diffOps, baseline, current, segments);
        
        // 合并相邻的相同类型段
        segments = mergeAdjacentSegments(segments);

        return new InlineDiffResult(baseline, current, segments);
    }
    
    /**
     * 计算字符级差异操作
     */
    private List<CharDiffOp> computeCharacterLevelDiff(String baseline, String current) {
        int m = baseline.length();
        int n = current.length();
        
        // 使用动态规划计算LCS
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (baseline.charAt(i - 1) == current.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        
        // 回溯构建差异操作序列
        List<CharDiffOp> ops = new ArrayList<>();
        int i = m, j = n;
        
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && baseline.charAt(i - 1) == current.charAt(j - 1)) {
                ops.add(0, new CharDiffOp(CharDiffOpType.EQUAL, i - 1, j - 1, baseline.charAt(i - 1)));
                i--;
                j--;
            } else if (i > 0 && (j == 0 || dp[i - 1][j] >= dp[i][j - 1])) {
                ops.add(0, new CharDiffOp(CharDiffOpType.DELETE, i - 1, -1, baseline.charAt(i - 1)));
                i--;
            } else {
                ops.add(0, new CharDiffOp(CharDiffOpType.INSERT, -1, j - 1, current.charAt(j - 1)));
                j--;
            }
        }
        
        return ops;
    }
    
    /**
     * 将diff操作转换为段
     */
    private void convertDiffOpsToSegments(List<CharDiffOp> diffOps, String baseline, String current, List<InlineDiffSegment> segments) {
        StringBuilder baselineText = new StringBuilder();
        StringBuilder currentText = new StringBuilder();
        CharDiffOpType currentType = null;
        int baselineStart = 0, currentStart = 0;
        
        for (CharDiffOp op : diffOps) {
            if (currentType != null && currentType != op.type) {
                // 类型变化，保存当前段
                addSegment(segments, currentType, baselineText.toString(), currentText.toString(), 
                          baselineStart, baselineStart + baselineText.length(),
                          currentStart, currentStart + currentText.length());
                
                // 重置状态
                baselineStart += baselineText.length();
                currentStart += currentText.length();
                baselineText.setLength(0);
                currentText.setLength(0);
            }
            
            currentType = op.type;
            
            switch (op.type) {
                case EQUAL:
                    baselineText.append(op.character);
                    currentText.append(op.character);
                    break;
                case DELETE:
                    baselineText.append(op.character);
                    break;
                case INSERT:
                    currentText.append(op.character);
                    break;
            }
        }
        
        // 添加最后一个段
        if (currentType != null) {
            addSegment(segments, currentType, baselineText.toString(), currentText.toString(),
                      baselineStart, baselineStart + baselineText.length(),
                      currentStart, currentStart + currentText.length());
        }
    }
    
    /**
     * 添加段
     */
    private void addSegment(List<InlineDiffSegment> segments, CharDiffOpType type, 
                           String baselineText, String currentText,
                           int baselineStart, int baselineEnd, int currentStart, int currentEnd) {
        InlineDiffType segmentType;
        switch (type) {
            case EQUAL:
                segmentType = InlineDiffType.EQUAL;
                break;
            case DELETE:
                segmentType = InlineDiffType.DELETE;
                break;
            case INSERT:
                segmentType = InlineDiffType.ADD;
                break;
            default:
                return;
        }
        
        segments.add(new InlineDiffSegment(segmentType, baselineText, currentText,
                                          baselineStart, baselineEnd, currentStart, currentEnd));
    }
    
    /**
     * 合并相邻的相同类型段
     */
    private List<InlineDiffSegment> mergeAdjacentSegments(List<InlineDiffSegment> segments) {
        if (segments.isEmpty()) {
            return segments;
        }
        
        List<InlineDiffSegment> merged = new ArrayList<>();
        InlineDiffSegment current = segments.get(0);
        
        for (int i = 1; i < segments.size(); i++) {
            InlineDiffSegment next = segments.get(i);
            
            if (current.getType() == next.getType()) {
                // 相同类型，合并
                current = new InlineDiffSegment(
                    current.getType(),
                    current.getBaselineText() + next.getBaselineText(),
                    current.getCurrentText() + next.getCurrentText(),
                    current.getBaselineStart(),
                    next.getBaselineEnd(),
                    current.getCurrentStart(),
                    next.getCurrentEnd()
                );
            } else {
                // 不同类型，保存当前段
                merged.add(current);
                current = next;
            }
        }
        
        merged.add(current);
        return merged;
    }

    /**
     * 计算字符级LCS矩阵
     */
    private int[][] computeCharLCS(String baseline, String current) {
        int m = baseline.length();
        int n = current.length();
        int[][] lcs = new int[m + 1][n + 1];

        for (int i = 0; i <= m; i++) {
            for (int j = 0; j <= n; j++) {
                if (i == 0 || j == 0) {
                    lcs[i][j] = 0;
                } else if (baseline.charAt(i - 1) == current.charAt(j - 1)) {
                    lcs[i][j] = lcs[i - 1][j - 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i - 1][j], lcs[i][j - 1]);
                }
            }
        }

        return lcs;
    }

    /**
     * 回溯字符级差异
     */
    private void backtrackCharDiff(int[][] lcs, String baseline, String current, int i, int j, List<InlineDiffSegment> segments) {
        if (i == 0 || j == 0) {
            return;
        }

        if (baseline.charAt(i - 1) == current.charAt(j - 1)) {
            // 相同字符，继续回溯
            backtrackCharDiff(lcs, baseline, current, i - 1, j - 1, segments);
        } else if (lcs[i - 1][j] >= lcs[i][j - 1]) {
            // 基线中有额外字符
            backtrackCharDiff(lcs, baseline, current, i - 1, j, segments);
            segments.add(new InlineDiffSegment(InlineDiffType.DELETE,
                                             String.valueOf(baseline.charAt(i - 1)), "",
                                             i - 1, i, j, j));
        } else {
            // 当前行中有额外字符
            backtrackCharDiff(lcs, baseline, current, i, j - 1, segments);
            segments.add(new InlineDiffSegment(InlineDiffType.ADD,
                                             "", String.valueOf(current.charAt(j - 1)),
                                             i, i, j - 1, j));
        }
    }

    /**
     * 行内差异结果类
     */
    private static class InlineDiffResult {
        private final String baseline;
        private final String current;
        private final List<InlineDiffSegment> diffSegments;

        public InlineDiffResult(String baseline, String current, List<InlineDiffSegment> diffSegments) {
            this.baseline = baseline;
            this.current = current;
            this.diffSegments = diffSegments;
        }

        public String getBaseline() { return baseline; }
        public String getCurrent() { return current; }
        public List<InlineDiffSegment> getDiffSegments() { return diffSegments; }
    }

    /**
     * 行内差异段类
     */
    private static class InlineDiffSegment {
        private final InlineDiffType type;
        private final String baselineText;
        private final String currentText;
        private final int baselineStart;
        private final int baselineEnd;
        private final int currentStart;
        private final int currentEnd;

        public InlineDiffSegment(InlineDiffType type, String baselineText, String currentText,
                               int baselineStart, int baselineEnd, int currentStart, int currentEnd) {
            this.type = type;
            this.baselineText = baselineText;
            this.currentText = currentText;
            this.baselineStart = baselineStart;
            this.baselineEnd = baselineEnd;
            this.currentStart = currentStart;
            this.currentEnd = currentEnd;
        }

        // Getters
        public InlineDiffType getType() { return type; }
        public String getBaselineText() { return baselineText; }
        public String getCurrentText() { return currentText; }
        public int getBaselineStart() { return baselineStart; }
        public int getBaselineEnd() { return baselineEnd; }
        public int getCurrentStart() { return currentStart; }
        public int getCurrentEnd() { return currentEnd; }
    }

    /**
     * 行内差异类型枚举
     */
    private enum InlineDiffType {
        ADD, DELETE, EQUAL
    }
    
    /**
     * 字符差异操作类型
     */
    private enum CharDiffOpType {
        EQUAL, DELETE, INSERT
    }
    
    /**
     * 字符差异操作
     */
    private static class CharDiffOp {
        final CharDiffOpType type;
        final int baselineIndex;
        final int currentIndex;
        final char character;
        
        CharDiffOp(CharDiffOpType type, int baselineIndex, int currentIndex, char character) {
            this.type = type;
            this.baselineIndex = baselineIndex;
            this.currentIndex = currentIndex;
            this.character = character;
        }
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
    
    /**
     * 检测是否为JSON内容
     */
    private boolean isJsonContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = content.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || 
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }
    
    /**
     * 规范化JSON格式
     * 统一缩进、冒号后空格等格式问题
     */
    private String normalizeJsonFormat(String jsonContent) {
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return jsonContent;
        }
        
        try {
            // 使用Jackson进行格式化，统一格式
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Object jsonObject = mapper.readValue(jsonContent, Object.class);
            
            // 使用统一的格式输出：2空格缩进，冒号后无空格
            return mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(jsonObject);
        } catch (Exception e) {
            log.warn("JSON格式规范化失败，使用原始内容: {}", e.getMessage());
            return jsonContent;
        }
    }
}
