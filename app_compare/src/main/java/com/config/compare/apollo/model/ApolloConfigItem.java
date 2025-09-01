package com.config.compare.apollo.model;

import lombok.Data;

/**
 * Apollo配置项
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Data
public class ApolloConfigItem {

    /**
     * 配置项键
     */
    private String key;

    /**
     * 配置项值
     */
    private String value;

    /**
     * 配置项注释
     */
    private String comment;

    /**
     * 数据修改时间
     */
    private String dataChangeLastModifiedTime;

    /**
     * 数据修改人
     */
    private String dataChangeLastModifiedBy;

    /**
     * 数据创建时间
     */
    private String dataChangeCreatedTime;

    /**
     * 数据创建人
     */
    private String dataChangeCreatedBy;

    public ApolloConfigItem() {}

    public ApolloConfigItem(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public ApolloConfigItem(String key, String value, String comment) {
        this.key = key;
        this.value = value;
        this.comment = comment;
    }
}