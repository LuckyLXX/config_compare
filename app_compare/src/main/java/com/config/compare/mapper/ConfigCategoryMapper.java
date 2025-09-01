package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.ConfigCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 配置分类Mapper
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Mapper
public interface ConfigCategoryMapper extends BaseMapper<ConfigCategory> {

    /**
     * 根据父分类ID查询子分类列表
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<ConfigCategory> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据服务器类型ID查询适用的配置分类列表
     * 
     * @param serverTypeId 服务器类型ID
     * @return 配置分类列表
     */
    List<ConfigCategory> selectByServerTypeId(@Param("serverTypeId") Long serverTypeId);

    /**
     * 查询所有启用的配置分类（树形结构）
     * 
     * @return 配置分类列表
     */
    List<ConfigCategory> selectEnabledCategories();
}