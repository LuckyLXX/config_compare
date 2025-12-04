package com.config.compare.compare.algorithm.impl;

import com.config.compare.compare.algorithm.CompareAlgorithm;
import com.config.compare.compare.model.CompareContext;
import com.config.compare.compare.model.CompareResultModel;
import com.config.compare.compare.model.DiffItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * JSON比对算法
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Component
public class JsonCompareAlgorithm implements CompareAlgorithm {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getAlgorithmType() {
        return "JSON";
    }

    @Override
    public String getAlgorithmName() {
        return "JSON结构比对算法";
    }

    @Override
    public CompareResultModel compare(CompareContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("开始执行JSON比对，基线长度：{}，当前内容长度：{}", 
                     context.getBaselineContent() != null ? context.getBaselineContent().length() : 0,
                     context.getCurrentContent() != null ? context.getCurrentContent().length() : 0);
            
            CompareResultModel result = doJsonCompare(context);
            result.setAlgorithmType(getAlgorithmType());
            result.setDuration(startTime);
            
            log.debug("JSON比对完成，一致性：{}，差异数量：{}", result.isConsistent(), result.getDiffCount());
            
            return result;
            
        } catch (Exception e) {
            log.error("JSON比对过程发生异常", e);
            CompareResultModel result = CompareResultModel.fail("JSON比对异常：" + e.getMessage());
            result.setAlgorithmType(getAlgorithmType());
            result.setDuration(startTime);
            return result;
        }
    }

    @Override
    public boolean supports(String contentType) {
        // 暂时禁用JSON结构化比对，统一使用文本比对
        // return "JSON".equalsIgnoreCase(contentType) ||
        //        "APPLICATION_JSON".equalsIgnoreCase(contentType);
        return false;
    }

    @Override
    public String getDescription() {
        return "基于JSON结构的差异比对算法，支持JSON配置文件、API响应等JSON格式内容的比对";
    }

    /**
     * 执行JSON比对
     */
    private CompareResultModel doJsonCompare(CompareContext context) {
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
        
        try {
            // 解析JSON
            JsonNode baselineNode = objectMapper.readTree(baseline);
            JsonNode currentNode = objectMapper.readTree(current);
            
            CompareResultModel result = CompareResultModel.success(false);
            
            // 比对JSON节点
            compareJsonNodes(baselineNode, currentNode, "", result);
            
            // 计算总节点数用于评分
            int totalNodes = countJsonNodes(baselineNode) + countJsonNodes(currentNode);
            result.calculateConsistencyScore(Math.max(totalNodes, 1));
            
            // 生成差异摘要
            result.setDiffSummary(generateJsonDiffSummary(result));
            
            return result;
            
        } catch (Exception e) {
            log.error("JSON解析失败", e);
            return CompareResultModel.fail("JSON格式错误：" + e.getMessage());
        }
    }

    /**
     * 比对JSON节点
     */
    private void compareJsonNodes(JsonNode baseline, JsonNode current, String path, CompareResultModel result) {
        if (baseline == null && current == null) {
            return;
        }
        
        if (baseline == null) {
            // 新增节点
            addDiffItem(result, "ADD", path, null, nodeToString(current), "MEDIUM");
            return;
        }
        
        if (current == null) {
            // 删除节点
            addDiffItem(result, "DELETE", path, nodeToString(baseline), null, "MEDIUM");
            return;
        }
        
        // 节点类型不同
        if (baseline.getNodeType() != current.getNodeType()) {
            addDiffItem(result, "MODIFY", path, 
                       baseline.getNodeType().toString(), 
                       current.getNodeType().toString(), "HIGH");
            return;
        }
        
        // 根据节点类型进行比对
        if (baseline.isObject()) {
            compareObjectNodes(baseline, current, path, result);
        } else if (baseline.isArray()) {
            compareArrayNodes(baseline, current, path, result);
        } else {
            // 叶子节点比对
            if (!baseline.equals(current)) {
                String level = getJsonDiffLevel(path, nodeToString(baseline), nodeToString(current));
                addDiffItem(result, "MODIFY", path, nodeToString(baseline), nodeToString(current), level);
            }
        }
    }

    /**
     * 比对对象节点
     */
    private void compareObjectNodes(JsonNode baseline, JsonNode current, String path, CompareResultModel result) {
        // 检查baseline中的字段
        Iterator<Map.Entry<String, JsonNode>> baselineFields = baseline.fields();
        while (baselineFields.hasNext()) {
            Map.Entry<String, JsonNode> entry = baselineFields.next();
            String fieldName = entry.getKey();
            String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
            
            JsonNode baselineValue = entry.getValue();
            JsonNode currentValue = current.get(fieldName);
            
            compareJsonNodes(baselineValue, currentValue, fieldPath, result);
        }
        
        // 检查current中新增的字段
        Iterator<Map.Entry<String, JsonNode>> currentFields = current.fields();
        while (currentFields.hasNext()) {
            Map.Entry<String, JsonNode> entry = currentFields.next();
            String fieldName = entry.getKey();
            
            if (!baseline.has(fieldName)) {
                String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                addDiffItem(result, "ADD", fieldPath, null, nodeToString(entry.getValue()), "MEDIUM");
            }
        }
    }

    /**
     * 比对数组节点
     */
    private void compareArrayNodes(JsonNode baseline, JsonNode current, String path, CompareResultModel result) {
        int baselineSize = baseline.size();
        int currentSize = current.size();
        int maxSize = Math.max(baselineSize, currentSize);
        
        for (int i = 0; i < maxSize; i++) {
            String indexPath = path + "[" + i + "]";
            
            JsonNode baselineItem = i < baselineSize ? baseline.get(i) : null;
            JsonNode currentItem = i < currentSize ? current.get(i) : null;
            
            compareJsonNodes(baselineItem, currentItem, indexPath, result);
        }
    }

    /**
     * 添加差异项
     */
    private void addDiffItem(CompareResultModel result, String diffType, String path, 
                           String baselineValue, String currentValue, String level) {
        DiffItem diffItem = DiffItem.createWithPath(diffType, path, extractKey(path), 
                                                   baselineValue, currentValue, level);
        diffItem.setDiffCategory("JSON结构差异");
        diffItem.setSuggestAction(generateSuggestAction(diffType, path));
        result.addDiffItem(diffItem);
    }

    /**
     * 提取键名
     */
    private String extractKey(String path) {
        if (path.isEmpty()) {
            return "root";
        }
        
        int lastDotIndex = path.lastIndexOf(".");
        if (lastDotIndex >= 0) {
            return path.substring(lastDotIndex + 1);
        }
        
        return path;
    }

    /**
     * 生成建议操作
     */
    private String generateSuggestAction(String diffType, String path) {
        switch (diffType) {
            case "ADD":
                return "确认新增配置项 " + path + " 是否正确";
            case "DELETE":
                return "确认删除配置项 " + path + " 是否必要";
            case "MODIFY":
                return "检查配置项 " + path + " 的值变更是否符合预期";
            default:
                return "请检查配置项 " + path;
        }
    }

    /**
     * 判断JSON差异级别
     */
    private String getJsonDiffLevel(String path, String baselineValue, String currentValue) {
        String lowerPath = path.toLowerCase();
        
        // 敏感配置为高级别
        if (lowerPath.contains("password") || lowerPath.contains("secret") || 
            lowerPath.contains("token") || lowerPath.contains("key") ||
            lowerPath.contains("host") || lowerPath.contains("port") ||
            lowerPath.contains("database") || lowerPath.contains("redis")) {
            return "HIGH";
        }
        
        // 数值变化较大的为高级别
        try {
            double baseline = Double.parseDouble(baselineValue);
            double current = Double.parseDouble(currentValue);
            double changeRate = Math.abs((current - baseline) / baseline);
            if (changeRate > 0.5) { // 变化超过50%
                return "HIGH";
            }
        } catch (NumberFormatException e) {
            // 非数值类型忽略
        }
        
        // 其他为中级别
        return "MEDIUM";
    }

    /**
     * 节点转字符串
     */
    private String nodeToString(JsonNode node) {
        if (node == null) {
            return null;
        }
        
        if (node.isTextual()) {
            return node.textValue();
        }
        
        return node.toString();
    }

    /**
     * 统计JSON节点数量
     */
    private int countJsonNodes(JsonNode node) {
        if (node == null) {
            return 0;
        }
        
        int count = 1;
        
        if (node.isObject()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                count += countJsonNodes(elements.next());
            }
        } else if (node.isArray()) {
            Iterator<JsonNode> elements = node.elements();
            while (elements.hasNext()) {
                count += countJsonNodes(elements.next());
            }
        }
        
        return count;
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
        diffItem.setSuggestAction("检查JSON配置采集是否正常");
        return diffItem;
    }

    /**
     * 生成JSON差异摘要
     */
    private String generateJsonDiffSummary(CompareResultModel result) {
        StringBuilder summary = new StringBuilder();
        summary.append("JSON比对结果：");
        summary.append("总差异=").append(result.getDiffCount());
        
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
}