package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.CollectTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采集模板Mapper
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Mapper
public interface CollectTemplateMapper extends BaseMapper<CollectTemplate> {

    /**
     * 根据模板类型查询模板列表
     * 
     * @param templateType 模板类型
     * @return 模板列表
     */
    List<CollectTemplate> selectByTemplateType(@Param("templateType") String templateType);

    /**
     * 查询所有启用的模板
     * 
     * @return 模板列表
     */
    List<CollectTemplate> selectEnabledTemplates();

    /**
     * 根据服务器类型ID查询适用的模板列表
     * 
     * @param serverTypeId 服务器类型ID
     * @return 模板列表
     */
    List<CollectTemplate> selectByServerTypeId(@Param("serverTypeId") Long serverTypeId);
}