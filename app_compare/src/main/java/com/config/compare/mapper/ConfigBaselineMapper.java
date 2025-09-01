package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.ConfigBaseline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 配置基线Mapper
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Mapper
public interface ConfigBaselineMapper extends BaseMapper<ConfigBaseline> {

    /**
     * 根据系统ID、服务器类型ID和配置分类ID查询基线列表
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @return 基线列表
     */
    List<ConfigBaseline> selectBySystemTypeCategory(@Param("systemId") Long systemId,
                                                    @Param("serverTypeId") Long serverTypeId,
                                                    @Param("categoryId") Long categoryId);

    /**
     * 查询默认基线
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @return 默认基线
     */
    ConfigBaseline selectDefaultBaseline(@Param("systemId") Long systemId,
                                        @Param("serverTypeId") Long serverTypeId,
                                        @Param("categoryId") Long categoryId);

    /**
     * 根据基线名称查询基线
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @param baselineName 基线名称
     * @return 基线信息
     */
    ConfigBaseline selectByName(@Param("systemId") Long systemId,
                               @Param("serverTypeId") Long serverTypeId,
                               @Param("categoryId") Long categoryId,
                               @Param("baselineName") String baselineName);

    /**
     * 更新默认基线状态
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @param newDefaultId 新默认基线ID
     * @param oldDefaultId 原默认基线ID
     * @return 更新行数
     */
    int updateDefaultBaseline(@Param("systemId") Long systemId,
                             @Param("serverTypeId") Long serverTypeId,
                             @Param("categoryId") Long categoryId,
                             @Param("newDefaultId") Long newDefaultId,
                             @Param("oldDefaultId") Long oldDefaultId);
}