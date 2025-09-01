package com.config.compare.compare.model;

import lombok.Data;

/**
 * 差异项模型
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class DiffItem {

    /**
     * 差异类型：ADD/DELETE/MODIFY
     */
    private String diffType;

    /**
     * 差异路径
     */
    private String diffPath;

    /**
     * 差异键名
     */
    private String diffKey;

    /**
     * 基线值
     */
    private String baselineValue;

    /**
     * 当前值
     */
    private String currentValue;

    /**
     * 差异级别：HIGH/MEDIUM/LOW
     */
    private String diffLevel;

    /**
     * 差异分类
     */
    private String diffCategory;

    /**
     * 建议操作
     */
    private String suggestAction;

    /**
     * 差异描述
     */
    private String description;

    public DiffItem() {}

    public DiffItem(String diffType, String diffKey, String baselineValue, String currentValue) {
        this.diffType = diffType;
        this.diffKey = diffKey;
        this.baselineValue = baselineValue;
        this.currentValue = currentValue;
        this.diffLevel = "MEDIUM"; // 默认中等级别
    }

    public DiffItem(String diffType, String diffPath, String diffKey, String baselineValue, String currentValue, String diffLevel) {
        this.diffType = diffType;
        this.diffPath = diffPath;
        this.diffKey = diffKey;
        this.baselineValue = baselineValue;
        this.currentValue = currentValue;
        this.diffLevel = diffLevel;
    }

    /**
     * 创建新增类型差异
     */
    public static DiffItem createAdd(String key, String currentValue) {
        return new DiffItem("ADD", key, null, currentValue);
    }

    /**
     * 创建删除类型差异
     */
    public static DiffItem createDelete(String key, String baselineValue) {
        return new DiffItem("DELETE", key, baselineValue, null);
    }

    /**
     * 创建修改类型差异
     */
    public static DiffItem createModify(String key, String baselineValue, String currentValue) {
        return new DiffItem("MODIFY", key, baselineValue, currentValue);
    }

    /**
     * 创建带路径的差异项
     */
    public static DiffItem createWithPath(String diffType, String path, String key, String baselineValue, String currentValue, String level) {
        return new DiffItem(diffType, path, key, baselineValue, currentValue, level);
    }

    /**
     * 生成差异描述
     */
    public String generateDescription() {
        if (description != null) {
            return description;
        }
        
        StringBuilder desc = new StringBuilder();
        String keyPath = diffPath != null ? diffPath + "." + diffKey : diffKey;
        
        switch (diffType) {
            case "ADD":
                desc.append("新增配置项：").append(keyPath).append(" = ").append(currentValue);
                break;
            case "DELETE":
                desc.append("删除配置项：").append(keyPath).append("（原值：").append(baselineValue).append("）");
                break;
            case "MODIFY":
                desc.append("修改配置项：").append(keyPath)
                    .append("，原值：").append(baselineValue)
                    .append("，新值：").append(currentValue);
                break;
        }
        
        return desc.toString();
    }
}