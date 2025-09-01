package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ConfigCategory;

import java.util.List;

/**
 * 配置分类Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ConfigCategoryService extends IService<ConfigCategory> {

    /**
     * 分页查询配置分类
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果
     */
    IPage<ConfigCategory> pageQuery(PageRequest pageRequest);

    /**
     * 获取所有启用的配置分类（平铺列表）
     * 
     * @return 启用的配置分类列表
     */
    List<ConfigCategory> listEnabled();

    /**
     * 获取所有启用的配置分类（树形结构）
     * 
     * @return 配置分类树
     */
    List<ConfigCategory> getCategoryTree();

    /**
     * 根据父分类ID查询子分类列表
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<ConfigCategory> listByParentId(Long parentId);

    /**
     * 根据服务器类型ID查询适用的配置分类列表
     * 
     * @param serverTypeId 服务器类型ID
     * @return 配置分类列表
     */
    List<ConfigCategory> listByServerTypeId(Long serverTypeId);

    /**
     * 根据系统ID查询适用的配置分类列表
     * 
     * @param systemId 系统ID
     * @return 配置分类列表
     */
    List<ConfigCategory> listBySystemId(Long systemId);

    /**
     * 检查分类编码是否存在
     * 
     * @param categoryCode 分类编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkCategoryCodeExists(String categoryCode, Long excludeId);

    /**
     * 创建配置分类
     * 
     * @param configCategory 配置分类
     * @return 创建结果
     */
    boolean createCategory(ConfigCategory configCategory);

    /**
     * 更新配置分类
     * 
     * @param configCategory 配置分类
     * @return 更新结果
     */
    boolean updateCategory(ConfigCategory configCategory);

    /**
     * 删除配置分类
     * 
     * @param id 分类ID
     * @return 删除结果
     */
    boolean deleteCategory(Long id);

    /**
     * 启用/禁用配置分类
     * 
     * @param id 分类ID
     * @param status 状态（1启用，0禁用）
     * @return 操作结果
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 检查分类是否有子分类
     * 
     * @param id 分类ID
     * @return 是否有子分类
     */
    boolean hasChildren(Long id);

    /**
     * 检查分类是否被基线使用
     * 
     * @param id 分类ID
     * @return 是否被使用
     */
    boolean isUsedByBaseline(Long id);
}