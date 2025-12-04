package com.config.compare.compare.algorithm.impl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Myers差分算法实现
 * 基于Eugene W. Myers的"An O(ND) Difference Algorithm and Its Variations"论文
 * 这是Beyond Compare等专业工具使用的核心算法
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-10-29
 */
@Slf4j
public class MyersDiffAlgorithm {

    /**
     * 差异操作类型
     */
    public enum DiffOperation {
        EQUAL,    // 相等
        DELETE,   // 删除
        INSERT    // 插入
    }

    /**
     * 差异操作
     */
    @Data
    public static class DiffOp {
        private DiffOperation operation;
        private String text;
        private int baselineIndex;
        private int currentIndex;

        public DiffOp(DiffOperation operation, String text) {
            this.operation = operation;
            this.text = text;
            this.baselineIndex = -1;
            this.currentIndex = -1;
        }

        public DiffOp(DiffOperation operation, String text, int baselineIndex, int currentIndex) {
            this.operation = operation;
            this.text = text;
            this.baselineIndex = baselineIndex;
            this.currentIndex = currentIndex;
        }
    }

    /**
     * 路径节点（用于Myers算法的路径追踪）
     */
    @Data
    private static class PathNode {
        private PathNode previousNode;
        private int x;
        private int y;

        public PathNode(int x, int y, PathNode previousNode) {
            this.x = x;
            this.y = y;
            this.previousNode = previousNode;
        }
    }

    /**
     * 使用Myers算法计算两个文本列表的差异
     * 
     * @param baseline 基线文本行列表
     * @param current 当前文本行列表
     * @return 差异操作列表
     */
    public static List<DiffOp> computeDiff(List<String> baseline, List<String> current) {
        if (baseline == null) baseline = new ArrayList<>();
        if (current == null) current = new ArrayList<>();

        log.debug("🔍 使用Myers算法计算差异: baseline={} lines, current={} lines", 
                 baseline.size(), current.size());

        // 使用Myers算法计算最短编辑路径
        PathNode path = computeShortestEditScript(baseline, current);
        
        // 从路径构建差异操作列表
        List<DiffOp> diffs = buildDiffFromPath(path, baseline, current);
        
        log.debug("✅ Myers算法计算完成: {} operations", diffs.size());
        return diffs;
    }

    /**
     * Myers算法核心：计算最短编辑脚本
     */
    private static PathNode computeShortestEditScript(List<String> baseline, List<String> current) {
        int n = baseline.size();
        int m = current.size();
        int max = n + m;

        // V数组存储每个k值对应的最远x坐标
        int[] v = new int[2 * max + 1];
        // 路径追踪数组
        PathNode[] path = new PathNode[2 * max + 1];

        v[max + 1] = 0;
        path[max + 1] = new PathNode(0, 0, null);

        for (int d = 0; d <= max; d++) {
            for (int k = -d; k <= d; k += 2) {
                int kIndex = k + max;
                
                int x;
                PathNode previousNode;

                if (k == -d || (k != d && v[kIndex - 1] < v[kIndex + 1])) {
                    // 向下移动（插入）
                    x = v[kIndex + 1];
                    previousNode = path[kIndex + 1];
                } else {
                    // 向右移动（删除）
                    x = v[kIndex - 1] + 1;
                    previousNode = path[kIndex - 1];
                }

                int y = x - k;

                // 沿对角线前进（匹配）
                while (x < n && y < m && baseline.get(x).equals(current.get(y))) {
                    x++;
                    y++;
                }

                v[kIndex] = x;
                path[kIndex] = new PathNode(x, y, previousNode);

                // 如果到达终点，返回路径
                if (x >= n && y >= m) {
                    return path[kIndex];
                }
            }
        }

        // 理论上不应该到达这里
        return path[max + 1];
    }

    /**
     * 从路径构建差异操作列表
     */
    private static List<DiffOp> buildDiffFromPath(PathNode endNode, List<String> baseline, List<String> current) {
        List<DiffOp> diffs = new ArrayList<>();
        
        // 收集路径节点
        List<PathNode> pathNodes = new ArrayList<>();
        PathNode currentNode = endNode;
        while (currentNode != null) {
            pathNodes.add(currentNode);
            currentNode = currentNode.previousNode;
        }
        
        // 反转路径（从起点到终点）
        Collections.reverse(pathNodes);

        // 构建差异操作
        for (int i = 1; i < pathNodes.size(); i++) {
            PathNode prev = pathNodes.get(i - 1);
            PathNode curr = pathNodes.get(i);

            int deltaX = curr.x - prev.x;
            int deltaY = curr.y - prev.y;

            if (deltaX > 0 && deltaY > 0) {
                // 对角线移动：先处理删除/插入，再处理相等
                if (deltaX > deltaY) {
                    // 更多删除
                    for (int j = 0; j < deltaX - deltaY; j++) {
                        diffs.add(new DiffOp(DiffOperation.DELETE, 
                                           baseline.get(prev.x + j), 
                                           prev.x + j, -1));
                    }
                    // 然后是相等的部分
                    for (int j = 0; j < deltaY; j++) {
                        diffs.add(new DiffOp(DiffOperation.EQUAL, 
                                           baseline.get(prev.x + deltaX - deltaY + j),
                                           prev.x + deltaX - deltaY + j, 
                                           prev.y + j));
                    }
                } else if (deltaY > deltaX) {
                    // 更多插入
                    for (int j = 0; j < deltaY - deltaX; j++) {
                        diffs.add(new DiffOp(DiffOperation.INSERT, 
                                           current.get(prev.y + j), 
                                           -1, prev.y + j));
                    }
                    // 然后是相等的部分
                    for (int j = 0; j < deltaX; j++) {
                        diffs.add(new DiffOp(DiffOperation.EQUAL, 
                                           baseline.get(prev.x + j),
                                           prev.x + j, 
                                           prev.y + deltaY - deltaX + j));
                    }
                } else {
                    // 相等数量的匹配
                    for (int j = 0; j < deltaX; j++) {
                        diffs.add(new DiffOp(DiffOperation.EQUAL, 
                                           baseline.get(prev.x + j),
                                           prev.x + j, prev.y + j));
                    }
                }
            } else if (deltaX > 0) {
                // 只有删除
                for (int j = 0; j < deltaX; j++) {
                    diffs.add(new DiffOp(DiffOperation.DELETE, 
                                       baseline.get(prev.x + j), 
                                       prev.x + j, -1));
                }
            } else if (deltaY > 0) {
                // 只有插入
                for (int j = 0; j < deltaY; j++) {
                    diffs.add(new DiffOp(DiffOperation.INSERT, 
                                       current.get(prev.y + j), 
                                       -1, prev.y + j));
                }
            }
        }

        return diffs;
    }

    /**
     * 计算字符级别的差异（用于行内差异分析）
     */
    public static List<DiffOp> computeCharacterDiff(String baseline, String current) {
        if (baseline == null) baseline = "";
        if (current == null) current = "";

        // 将字符串转换为字符列表
        List<String> baselineChars = new ArrayList<>();
        List<String> currentChars = new ArrayList<>();

        for (char c : baseline.toCharArray()) {
            baselineChars.add(String.valueOf(c));
        }
        for (char c : current.toCharArray()) {
            currentChars.add(String.valueOf(c));
        }

        return computeDiff(baselineChars, currentChars);
    }

    /**
     * 优化差异结果：合并相邻的相同操作
     */
    public static List<DiffOp> optimizeDiffs(List<DiffOp> diffs) {
        if (diffs == null || diffs.isEmpty()) {
            return new ArrayList<>();
        }

        List<DiffOp> optimized = new ArrayList<>();
        DiffOp current = null;

        for (DiffOp diff : diffs) {
            if (current == null || current.operation != diff.operation) {
                // 新的操作类型
                if (current != null) {
                    optimized.add(current);
                }
                current = new DiffOp(diff.operation, diff.text, diff.baselineIndex, diff.currentIndex);
            } else {
                // 相同操作类型，合并文本
                current.text += diff.text;
            }
        }

        if (current != null) {
            optimized.add(current);
        }

        return optimized;
    }

    /**
     * 将差异结果转换为统计信息
     */
    public static DiffStatistics computeStatistics(List<DiffOp> diffs) {
        int insertions = 0;
        int deletions = 0;
        int equals = 0;

        for (DiffOp diff : diffs) {
            switch (diff.operation) {
                case INSERT:
                    insertions++;
                    break;
                case DELETE:
                    deletions++;
                    break;
                case EQUAL:
                    equals++;
                    break;
            }
        }

        return new DiffStatistics(insertions, deletions, equals);
    }

    /**
     * 差异统计信息
     */
    @Data
    public static class DiffStatistics {
        private int insertions;
        private int deletions;
        private int equals;
        private double similarity;

        public DiffStatistics(int insertions, int deletions, int equals) {
            this.insertions = insertions;
            this.deletions = deletions;
            this.equals = equals;
            
            int total = insertions + deletions + equals;
            this.similarity = total > 0 ? (double) equals / total : 1.0;
        }
    }
}
