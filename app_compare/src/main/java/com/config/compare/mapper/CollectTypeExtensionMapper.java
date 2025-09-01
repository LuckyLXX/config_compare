package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.CollectTypeExtension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采集类型扩展Mapper
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Mapper
public interface CollectTypeExtensionMapper extends BaseMapper<CollectTypeExtension> {

    /**
     * 根据类型分类查询采集类型列表
     * 
     * @param typeCategory 类型分类
     * @return 采集类型列表
     */
    List<CollectTypeExtension> selectByTypeCategory(@Param("typeCategory") String typeCategory);

    /**
     * 查询所有启用的采集类型
     * 
     * @return 采集类型列表
     */
    List<CollectTypeExtension> selectEnabledTypes();

    /**
     * 根据类型编码查询采集类型
     * 
     * @param typeCode 类型编码
     * @return 采集类型
     */
    CollectTypeExtension selectByTypeCode(@Param("typeCode") String typeCode);
}