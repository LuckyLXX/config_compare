package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ConfigCategory;
import com.config.compare.mapper.ConfigCategoryMapper;
import com.config.compare.service.ConfigCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置分类Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class ConfigCategoryServiceImpl extends ServiceImpl<ConfigCategoryMapper, ConfigCategory> implements ConfigCategoryService {

    @Override
    public IPage<ConfigCategory> pageQuery(PageRequest pageRequest) {
        Page<ConfigCategory> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ConfigCategory> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
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
        List<ConfigCategory> allCategories = baseMapper.selectEnabledCategories();
        return buildCategoryTree(allCategories, 0L);
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
        
        // 设置默认值
        if (configCategory.getParentId() == null) {
            configCategory.setParentId(0L);
        }
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
        
        // 防止将自己设置为父分类
        if (configCategory.getParentId() != null && configCategory.getParentId().equals(configCategory.getId())) {
            throw new RuntimeException("不能将自己设置为父分类");
        }
        
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
        // TODO: 这里需要检查config_baseline表中是否有使用该分类的基线
        // 暂时返回false，等ConfigBaseline相关功能完成后再实现
        return false;
    }

    /**
     * 构建分类树
     * 
     * @param categories 所有分类
     * @param parentId 父分类ID
     * @return 分类树
     */
    private List<ConfigCategory> buildCategoryTree(List<ConfigCategory> categories, Long parentId) {
        return categories.stream()
            .filter(category -> {
                Long categoryParentId = category.getParentId() == null ? 0L : category.getParentId();
                return categoryParentId.equals(parentId);
            })
            .peek(category -> {
                List<ConfigCategory> children = buildCategoryTree(categories, category.getId());
                // 这里可以设置children字段，如果ConfigCategory实体中有children字段的话
            })
            .collect(Collectors.toList());
    }
}