package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.CategoryPageRequest;
import com.config.compare.entity.ConfigBaseline;
import com.config.compare.entity.ConfigCategory;
import com.config.compare.mapper.ConfigBaselineMapper;
import com.config.compare.mapper.ConfigCategoryMapper;
import com.config.compare.service.ConfigCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 配置分类Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigCategoryServiceImpl extends ServiceImpl<ConfigCategoryMapper, ConfigCategory> implements ConfigCategoryService {

    private final ConfigBaselineMapper configBaselineMapper;

    @Override
    public IPage<ConfigCategory> pageQuery(CategoryPageRequest pageRequest) {
        Page<ConfigCategory> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ConfigCategory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 【新增】分类名称筛选
        if (StringUtils.hasText(pageRequest.getCategoryName())) {
            queryWrapper.like(ConfigCategory::getCategoryName, pageRequest.getCategoryName());
        }
        
        // 【新增】分类编码筛选
        if (StringUtils.hasText(pageRequest.getCategoryCode())) {
            queryWrapper.like(ConfigCategory::getCategoryCode, pageRequest.getCategoryCode());
        }
        
        // 【新增】状态筛选
        if (pageRequest.getStatus() != null) {
            queryWrapper.eq(ConfigCategory::getStatus, pageRequest.getStatus());
        }
        
        // 关键词搜索（保留原有功能，可同时使用）
        if (StringUtils.hasText(pageRequest.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(ConfigCategory::getCategoryName, pageRequest.getKeyword())
                .or()
                .like(ConfigCategory::getCategoryCode, pageRequest.getKeyword())
                .or()
                .like(ConfigCategory::getDescription, pageRequest.getKeyword())
            );
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getOrderBy())) {
            if ("ASC".equalsIgnoreCase(pageRequest.getOrderDirection())) {
                switch (pageRequest.getOrderBy()) {
                    case "categoryName":
                        queryWrapper.orderByAsc(ConfigCategory::getCategoryName);
                        break;
                    case "categoryCode":
                        queryWrapper.orderByAsc(ConfigCategory::getCategoryCode);
                        break;
                    case "sortOrder":
                        queryWrapper.orderByAsc(ConfigCategory::getSortOrder);
                        break;
                    case "createTime":
                        queryWrapper.orderByAsc(ConfigCategory::getCreateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(ConfigCategory::getSortOrder)
                                   .orderByAsc(ConfigCategory::getCreateTime);
                }
            } else {
                switch (pageRequest.getOrderBy()) {
                    case "categoryName":
                        queryWrapper.orderByDesc(ConfigCategory::getCategoryName);
                        break;
                    case "categoryCode":
                        queryWrapper.orderByDesc(ConfigCategory::getCategoryCode);
                        break;
                    case "sortOrder":
                        queryWrapper.orderByDesc(ConfigCategory::getSortOrder);
                        break;
                    case "createTime":
                        queryWrapper.orderByDesc(ConfigCategory::getCreateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(ConfigCategory::getSortOrder)
                                   .orderByDesc(ConfigCategory::getCreateTime);
                }
            }
        } else {
            queryWrapper.orderByAsc(ConfigCategory::getSortOrder)
                       .orderByDesc(ConfigCategory::getCreateTime);
        }
        
        return this.page(page, queryWrapper);
    }

    @Override
    public List<ConfigCategory> listEnabled() {
        LambdaQueryWrapper<ConfigCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigCategory::getStatus, 1)
                   .orderByAsc(ConfigCategory::getSortOrder)
                   .orderByAsc(ConfigCategory::getCategoryName);
        return this.list(queryWrapper);
    }

    @Override
    public List<ConfigCategory> getCategoryTree() {
        // 不再支持树形结构，直接返回平铺的分类列表
        return baseMapper.selectEnabledCategories();
    }

    @Override
    public List<ConfigCategory> listByParentId(Long parentId) {
        return baseMapper.selectByParentId(parentId);
    }

    @Override
    public List<ConfigCategory> listByServerTypeId(Long serverTypeId) {
        return baseMapper.selectByServerTypeId(serverTypeId);
    }

    @Override
    public List<ConfigCategory> listBySystemId(Long systemId) {
        // 根据系统ID获取所有启用的配置分类
        // 这里可以根据业务逻辑进行调整
        LambdaQueryWrapper<ConfigCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigCategory::getStatus, 1)
                   .orderByAsc(ConfigCategory::getSortOrder)
                   .orderByAsc(ConfigCategory::getCategoryName);
        
        // TODO: 这里需要根据系统ID进行过滤
        // 目前暂时返回所有启用的配置分类
        // 后续可以根据系统与配置分类的关联关系进行调整
        log.info("根据系统ID获取配置分类，systemId: {}, 暂时返回所有启用的配置分类", systemId);
        
        return this.list(queryWrapper);
    }

    @Override
    public boolean checkCategoryCodeExists(String categoryCode, Long excludeId) {
        LambdaQueryWrapper<ConfigCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigCategory::getCategoryCode, categoryCode);
        if (excludeId != null) {
            queryWrapper.ne(ConfigCategory::getId, excludeId);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean createCategory(ConfigCategory configCategory) {
        // 检查分类编码是否已存在
        if (checkCategoryCodeExists(configCategory.getCategoryCode(), null)) {
            throw new RuntimeException("分类编码已存在");
        }
        
        // 设置默认值 - 所有分类都是顶级分类，不支持子分类
        configCategory.setParentId(0L);
        
        if (configCategory.getStatus() == null) {
            configCategory.setStatus(1);
        }
        if (configCategory.getSortOrder() == null) {
            configCategory.setSortOrder(0);
        }
        
        return this.save(configCategory);
    }

    @Override
    public boolean updateCategory(ConfigCategory configCategory) {
        // 检查分类编码是否已存在
        if (checkCategoryCodeExists(configCategory.getCategoryCode(), configCategory.getId())) {
            throw new RuntimeException("分类编码已存在");
        }
        
        // 强制设置为顶级分类 - 不支持子分类功能
        configCategory.setParentId(0L);
        
        return this.updateById(configCategory);
    }

    @Override
    public boolean deleteCategory(Long id) {
        // 检查是否有子分类
        if (hasChildren(id)) {
            throw new RuntimeException("该分类下存在子分类，无法删除");
        }
        
        // 检查是否被基线使用
        if (isUsedByBaseline(id)) {
            throw new RuntimeException("该分类已被基线使用，无法删除");
        }
        
        return this.removeById(id);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        return this.lambdaUpdate()
            .eq(ConfigCategory::getId, id)
            .set(ConfigCategory::getStatus, status)
            .update();
    }

    @Override
    public boolean hasChildren(Long id) {
        LambdaQueryWrapper<ConfigCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigCategory::getParentId, id);
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean isUsedByBaseline(Long id) {
        // 检查config_baseline表中是否有使用该分类的基线
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getCategoryId, id);
        Long count = configBaselineMapper.selectCount(queryWrapper);
        
        if (count > 0) {
            log.warn("分类ID: {} 被 {} 个基线使用，无法删除", id, count);
        }
        
        return count > 0;
    }

}