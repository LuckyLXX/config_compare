package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ConfigBaseline;
import com.config.compare.entity.BaselineVersionLog;
import com.config.compare.mapper.ConfigBaselineMapper;
import com.config.compare.service.ConfigBaselineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置基线Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class ConfigBaselineServiceImpl extends ServiceImpl<ConfigBaselineMapper, ConfigBaseline> implements ConfigBaselineService {

    @Override
    public IPage<ConfigBaseline> pageQuery(PageRequest pageRequest) {
        Page<ConfigBaseline> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(pageRequest.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(ConfigBaseline::getBaselineName, pageRequest.getKeyword())
                .or()
                .like(ConfigBaseline::getBaselineVersion, pageRequest.getKeyword())
                .or()
                .like(ConfigBaseline::getDescription, pageRequest.getKeyword())
            );
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getOrderBy())) {
            if ("ASC".equalsIgnoreCase(pageRequest.getOrderDirection())) {
                switch (pageRequest.getOrderBy()) {
                    case "baselineName":
                        queryWrapper.orderByAsc(ConfigBaseline::getBaselineName);
                        break;
                    case "baselineVersion":
                        queryWrapper.orderByAsc(ConfigBaseline::getBaselineVersion);
                        break;
                    case "isDefault":
                        queryWrapper.orderByAsc(ConfigBaseline::getIsDefault);
                        break;
                    case "createTime":
                        queryWrapper.orderByAsc(ConfigBaseline::getCreateTime);
                        break;
                    default:
                        queryWrapper.orderByDesc(ConfigBaseline::getIsDefault)
                                   .orderByDesc(ConfigBaseline::getCreateTime);
                }
            } else {
                switch (pageRequest.getOrderBy()) {
                    case "baselineName":
                        queryWrapper.orderByDesc(ConfigBaseline::getBaselineName);
                        break;
                    case "baselineVersion":
                        queryWrapper.orderByDesc(ConfigBaseline::getBaselineVersion);
                        break;
                    case "isDefault":
                        queryWrapper.orderByDesc(ConfigBaseline::getIsDefault);
                        break;
                    case "createTime":
                        queryWrapper.orderByDesc(ConfigBaseline::getCreateTime);
                        break;
                    default:
                        queryWrapper.orderByDesc(ConfigBaseline::getIsDefault)
                                   .orderByDesc(ConfigBaseline::getCreateTime);
                }
            }
        } else {
            queryWrapper.orderByDesc(ConfigBaseline::getIsDefault)
                       .orderByDesc(ConfigBaseline::getCreateTime);
        }
        
        return this.page(page, queryWrapper);
    }

    @Override
    public List<ConfigBaseline> listBySystemTypeCategory(Long systemId, Long serverTypeId, Long categoryId) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .orderByDesc(ConfigBaseline::getIsDefault)
                   .orderByDesc(ConfigBaseline::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    public ConfigBaseline getDefaultBaseline(Long systemId, Long serverTypeId, Long categoryId) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getIsDefault, 1);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean checkBaselineNameExists(Long systemId, Long serverTypeId, Long categoryId, 
                                          String baselineName, Long excludeId) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getBaselineName, baselineName);
        
        if (excludeId != null) {
            queryWrapper.ne(ConfigBaseline::getId, excludeId);
        }
        
        ConfigBaseline baseline = this.getOne(queryWrapper);
        return baseline != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createBaseline(ConfigBaseline configBaseline) {
        // 检查基线名称是否已存在
        if (checkBaselineNameExists(configBaseline.getSystemId(), 
                                   configBaseline.getServerTypeId(),
                                   configBaseline.getCategoryId(),
                                   configBaseline.getBaselineName(), 
                                   null)) {
            throw new RuntimeException("该分组下已存在相同基线名称");
        }
        
        // 设置默认值
        if (configBaseline.getIsDefault() == null) {
            configBaseline.setIsDefault(0);
        }
        if (configBaseline.getStatus() == null) {
            configBaseline.setStatus(1); // 默认生效状态
        }
        
        // 如果是第一个基线或设置为默认基线，则设为默认
        ConfigBaseline currentDefault = getDefaultBaseline(configBaseline.getSystemId(),
                                                          configBaseline.getServerTypeId(),
                                                          configBaseline.getCategoryId());
        if (currentDefault == null || configBaseline.getIsDefault() == 1) {
            configBaseline.setIsDefault(1);
            // 如果有原默认基线，则取消其默认状态
            if (currentDefault != null) {
                this.lambdaUpdate()
                    .eq(ConfigBaseline::getId, currentDefault.getId())
                    .set(ConfigBaseline::getIsDefault, 0)
                    .update();
            }
        }
        
        return this.save(configBaseline);
    }

    @Override
    public boolean updateBaseline(ConfigBaseline configBaseline) {
        // 检查基线名称是否已存在
        if (checkBaselineNameExists(configBaseline.getSystemId(),
                                   configBaseline.getServerTypeId(),
                                   configBaseline.getCategoryId(),
                                   configBaseline.getBaselineName(),
                                   configBaseline.getId())) {
            throw new RuntimeException("该分组下已存在相同基线名称");
        }
        
        return this.updateById(configBaseline);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBaseline(Long id) {
        ConfigBaseline baseline = this.getById(id);
        if (baseline == null) {
            throw new RuntimeException("基线不存在");
        }
        
        // 如果是默认基线，检查是否还有其他基线
        if (baseline.getIsDefault() == 1) {
            List<ConfigBaseline> otherBaselines = listBySystemTypeCategory(
                baseline.getSystemId(), baseline.getServerTypeId(), baseline.getCategoryId());
            otherBaselines.removeIf(b -> b.getId().equals(id));
            
            if (!otherBaselines.isEmpty()) {
                // 设置最新的基线为默认
                ConfigBaseline newDefault = otherBaselines.get(0);
                this.lambdaUpdate()
                    .eq(ConfigBaseline::getId, newDefault.getId())
                    .set(ConfigBaseline::getIsDefault, 1)
                    .update();
            }
        }
        
        return this.removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDefaultBaseline(Long id, String reason) {
        ConfigBaseline newDefault = this.getById(id);
        if (newDefault == null) {
            throw new RuntimeException("基线不存在");
        }
        
        // 获取当前默认基线
        ConfigBaseline currentDefault = getDefaultBaseline(newDefault.getSystemId(),
                                                          newDefault.getServerTypeId(),
                                                          newDefault.getCategoryId());
        
        if (currentDefault != null && currentDefault.getId().equals(id)) {
            throw new RuntimeException("该基线已是默认基线");
        }
        
        // 更新默认基线状态
        int updateCount = baseMapper.updateDefaultBaseline(
            newDefault.getSystemId(),
            newDefault.getServerTypeId(),
            newDefault.getCategoryId(),
            id,
            currentDefault != null ? currentDefault.getId() : null
        );
        
        // 记录版本切换日志
        if (updateCount > 0) {
            // TODO: 这里可以记录BaselineVersionLog，暂时注释
            // recordVersionLog(currentDefault, newDefault, reason, "SWITCH");
        }
        
        return updateCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean copyBaseline(Long sourceId, String newName, String newVersion, String description) {
        ConfigBaseline source = this.getById(sourceId);
        if (source == null) {
            throw new RuntimeException("源基线不存在");
        }
        
        // 检查新基线名称是否已存在
        if (checkBaselineNameExists(source.getSystemId(),
                                   source.getServerTypeId(),
                                   source.getCategoryId(),
                                   newName,
                                   null)) {
            throw new RuntimeException("该分组下已存在相同基线名称");
        }
        
        // 复制基线
        ConfigBaseline newBaseline = new ConfigBaseline();
        BeanUtils.copyProperties(source, newBaseline);
        newBaseline.setId(null);
        newBaseline.setBaselineName(newName);
        newBaseline.setBaselineVersion(newVersion);
        newBaseline.setDescription(description);
        newBaseline.setIsDefault(0);  // 复制的基线不设为默认
        newBaseline.setCreateTime(LocalDateTime.now());
        newBaseline.setUpdateTime(LocalDateTime.now());
        
        return this.save(newBaseline);
    }

    @Override
    public String compareBaselines(Long baseline1Id, Long baseline2Id) {
        ConfigBaseline baseline1 = this.getById(baseline1Id);
        ConfigBaseline baseline2 = this.getById(baseline2Id);
        
        if (baseline1 == null || baseline2 == null) {
            throw new RuntimeException("基线不存在");
        }
        
        // TODO: 实现基线内容比较逻辑
        // 这里需要比较两个基线的配置内容，返回差异信息
        StringBuilder diff = new StringBuilder();
        diff.append("基线比较结果:\n");
        diff.append("基线1: ").append(baseline1.getBaselineName()).append(" (").append(baseline1.getBaselineVersion()).append(")\n");
        diff.append("基线2: ").append(baseline2.getBaselineName()).append(" (").append(baseline2.getBaselineVersion()).append(")\n");
        diff.append("比较功能待实现...\n");
        
        return diff.toString();
    }

    @Override
    public List<ConfigBaseline> getVersionHistory(Long systemId, Long serverTypeId, Long categoryId) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .orderByDesc(ConfigBaseline::getCreateTime);
        return this.list(queryWrapper);
    }
}