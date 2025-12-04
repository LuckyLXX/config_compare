package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.BaselinePageRequest;
import com.config.compare.entity.ConfigBaseline;
import com.config.compare.entity.BaselineVersionLog;
import com.config.compare.mapper.ConfigBaselineMapper;
import com.config.compare.service.BaselineVersionLogService;
import com.config.compare.service.ConfigBaselineService;
import com.config.compare.util.VersionGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.MessageDigest;
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
@RequiredArgsConstructor
public class ConfigBaselineServiceImpl extends ServiceImpl<ConfigBaselineMapper, ConfigBaseline> implements ConfigBaselineService {

    private final BaselineVersionLogService baselineVersionLogService;

    @Override
    public IPage<ConfigBaseline> pageQuery(BaselinePageRequest pageRequest) {
        Page<ConfigBaseline> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        
        // 【新增】系统ID筛选
        if (pageRequest.getSystemId() != null) {
            queryWrapper.eq(ConfigBaseline::getSystemId, pageRequest.getSystemId());
        }
        
        // 【新增】服务器类型ID筛选
        if (pageRequest.getServerTypeId() != null) {
            queryWrapper.eq(ConfigBaseline::getServerTypeId, pageRequest.getServerTypeId());
        }
        
        // 【新增】配置分类ID筛选
        if (pageRequest.getCategoryId() != null) {
            queryWrapper.eq(ConfigBaseline::getCategoryId, pageRequest.getCategoryId());
        }
        
        // 【新增】基线名称模糊查询
        if (StringUtils.hasText(pageRequest.getBaselineName())) {
            queryWrapper.like(ConfigBaseline::getBaselineName, pageRequest.getBaselineName());
        }
        
        // 【新增】状态筛选
        if (pageRequest.getStatus() != null) {
            queryWrapper.eq(ConfigBaseline::getStatus, pageRequest.getStatus());
        }
        
        // 【新增】是否默认筛选
        if (pageRequest.getIsDefault() != null) {
            queryWrapper.eq(ConfigBaseline::getIsDefault, pageRequest.getIsDefault());
        }
        
        // 关键词搜索（保留原有功能，可同时使用）
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
        
        log.info("分页查询基线: systemId={}, serverTypeId={}, categoryId={}, baselineName={}, status={}, isDefault={}, current={}, size={}",
            pageRequest.getSystemId(), pageRequest.getServerTypeId(), pageRequest.getCategoryId(),
            pageRequest.getBaselineName(), pageRequest.getStatus(), pageRequest.getIsDefault(),
            pageRequest.getCurrent(), pageRequest.getSize());
        
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

    /**
     * 获取默认基线（不指定baselineName）
     * 注意：如果同一配置组合下有多个不同名称的基线，此方法可能返回任意一个
     */
    @Override
    public ConfigBaseline getDefaultBaseline(Long systemId, Long serverTypeId, Long categoryId) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getIsDefault, 1)
                   .eq(ConfigBaseline::getStatus, 1)  // 只查询生效的基线
                   .orderByDesc(ConfigBaseline::getUpdateTime)
                   .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }
    
    /**
     * 获取默认基线（精确匹配baselineName）
     * 推荐使用此方法，以 systemId + serverTypeId + categoryId + baselineName 作为唯一标识
     */
    public ConfigBaseline getDefaultBaseline(Long systemId, Long serverTypeId, Long categoryId, String baselineName) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getBaselineName, baselineName)  // 精确匹配基线名称
                   .eq(ConfigBaseline::getIsDefault, 1)
                   .eq(ConfigBaseline::getStatus, 1)  // 只查询生效的基线
                   .orderByDesc(ConfigBaseline::getUpdateTime)
                   .last("LIMIT 1");
        
        ConfigBaseline baseline = this.getOne(queryWrapper);
        
        if (baseline != null) {
            log.debug("查询默认基线: 系统ID={}, 服务器类型ID={}, 配置分类ID={}, 基线名称={}, 找到基线: ID={}, 版本={}", 
                systemId, serverTypeId, categoryId, baselineName, baseline.getId(), baseline.getBaselineVersion());
        } else {
            log.debug("查询默认基线: 系统ID={}, 服务器类型ID={}, 配置分类ID={}, 基线名称={}, 未找到默认基线", 
                systemId, serverTypeId, categoryId, baselineName);
        }
        
        return baseline;
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
        
        // 【修改】改为归档而不是真正删除
        log.info("归档基线: {} ({}), 原状态: {}", baseline.getBaselineName(), baseline.getBaselineVersion(), baseline.getStatus());
        
        // 如果是默认基线，需要先设置其他基线为默认
        if (baseline.getIsDefault() == 1) {
            List<ConfigBaseline> otherBaselines = listBySystemTypeCategory(
                baseline.getSystemId(), baseline.getServerTypeId(), baseline.getCategoryId());
            otherBaselines.removeIf(b -> b.getId().equals(id) || b.getStatus() == 2); // 排除当前基线和已归档的基线
            
            if (!otherBaselines.isEmpty()) {
                // 设置最新的基线为默认
                ConfigBaseline newDefault = otherBaselines.get(0);
                this.lambdaUpdate()
                    .eq(ConfigBaseline::getId, newDefault.getId())
                    .set(ConfigBaseline::getIsDefault, 1)
                    .set(ConfigBaseline::getStatus, 1)
                    .update();
                log.info("新的默认基线: {} ({})", newDefault.getBaselineName(), newDefault.getBaselineVersion());
            }
        }
        
        // 归档基线：设置为非默认、归档状态
        return this.lambdaUpdate()
            .eq(ConfigBaseline::getId, id)
            .set(ConfigBaseline::getIsDefault, 0)
            .set(ConfigBaseline::getStatus, 2)
            .update();
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
    public List<ConfigBaseline> getVersionHistory(Long systemId, Long serverTypeId, Long categoryId, String baselineName) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getBaselineName, baselineName)  // 【关键】使用基线名称精确匹配
                   .orderByDesc(ConfigBaseline::getCreateTime);
        
        List<ConfigBaseline> history = this.list(queryWrapper);
        log.info("查询版本历史: 系统[{}], 服务器类型[{}], 配置分类[{}], 基线名称[{}], 共 {} 个版本", 
            systemId, serverTypeId, categoryId, baselineName, history.size());
        return history;
    }
    
    @Override
    public ConfigBaseline getDefaultBaselineWithoutServerType(Long systemId, Long categoryId) {
        LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getIsDefault, 1)
                   .eq(ConfigBaseline::getStatus, 1)
                   .orderByDesc(ConfigBaseline::getUpdateTime)
                   .last("LIMIT 1");
        
        ConfigBaseline baseline = this.getOne(queryWrapper);
        
        // 调试日志：记录查询到的基线信息
        if (baseline != null) {
            log.info("查询默认基线 - 系统ID:{}, 配置分类ID:{}, 找到基线: ID={}, 名称={}, 版本={}, 服务器类型ID={}, 更新时间={}", 
                systemId, categoryId, baseline.getId(), baseline.getBaselineName(), 
                baseline.getBaselineVersion(), baseline.getServerTypeId(), baseline.getUpdateTime());
        } else {
            log.warn("查询默认基线 - 系统ID:{}, 配置分类ID:{}, 未找到默认基线", systemId, categoryId);
        }
        
        // 检查是否有多个默认基线（用于诊断数据问题）
        LambdaQueryWrapper<ConfigBaseline> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(ConfigBaseline::getSystemId, systemId)
                   .eq(ConfigBaseline::getCategoryId, categoryId)
                   .eq(ConfigBaseline::getIsDefault, 1)
                   .eq(ConfigBaseline::getStatus, 1);
        long count = this.count(checkWrapper);
        if (count > 1) {
            log.warn("⚠️ 发现数据异常：系统ID:{}, 配置分类ID:{} 存在 {} 个默认基线！请检查数据一致性。", 
                systemId, categoryId, count);
        }
        
        return baseline;
    }

    @Override
    public String generateVersion(Long systemId, Long serverTypeId, Long categoryId) {
        // 使用VersionGenerator生成唯一版本号，检查版本号是否已存在
        String version = VersionGenerator.generateUniqueVersion(v -> {
            LambdaQueryWrapper<ConfigBaseline> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ConfigBaseline::getSystemId, systemId)
                       .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                       .eq(ConfigBaseline::getCategoryId, categoryId)
                       .eq(ConfigBaseline::getBaselineVersion, v);
            return this.count(queryWrapper) > 0;
        });
        
        log.info("为系统[{}]-服务器类型[{}]-配置分类[{}]生成版本号: {}", 
            systemId, serverTypeId, categoryId, version);
        return version;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfigBaseline promoteToBaseline(Long systemId, Long serverTypeId, Long categoryId, String baselineName,
                                           String currentContent, String fileName, String description) {
        log.info("开始晋级采集版本为基线: 系统[{}]-服务器类型[{}]-配置分类[{}]-基线名称[{}]", 
            systemId, serverTypeId, categoryId, baselineName);
        
        // 1. 查询当前默认基线（使用 systemId + serverTypeId + categoryId + baselineName 精确匹配）
        ConfigBaseline currentDefault = getDefaultBaseline(systemId, serverTypeId, categoryId, baselineName);
        
        // 2. 自动生成版本号
        String newVersion = generateVersion(systemId, serverTypeId, categoryId);
        
        // 3. 归档旧的默认基线
        // 使用 systemId + serverTypeId + categoryId + baselineName 作为唯一标识
        // 只归档生效状态（status=1）的默认基线
        LambdaQueryWrapper<ConfigBaseline> archiveWrapper = new LambdaQueryWrapper<>();
        archiveWrapper.eq(ConfigBaseline::getSystemId, systemId)
                     .eq(ConfigBaseline::getServerTypeId, serverTypeId)
                     .eq(ConfigBaseline::getCategoryId, categoryId)
                     .eq(ConfigBaseline::getBaselineName, baselineName)  // 【关键】使用基线名称精确匹配
                     .eq(ConfigBaseline::getIsDefault, 1)
                     .eq(ConfigBaseline::getStatus, 1);  // 只查询生效的基线
        List<ConfigBaseline> oldDefaults = this.list(archiveWrapper);
        
        if (!oldDefaults.isEmpty()) {
            log.info("归档 {} 个旧的生效默认基线（系统[{}]-服务器类型[{}]-配置分类[{}]-基线名称[{}]）", 
                oldDefaults.size(), systemId, serverTypeId, categoryId, baselineName);
            for (ConfigBaseline oldDefault : oldDefaults) {
                log.info("  - 归档基线: ID={}, 版本={}", 
                    oldDefault.getId(), oldDefault.getBaselineVersion());
                this.lambdaUpdate()
                    .eq(ConfigBaseline::getId, oldDefault.getId())
                    .set(ConfigBaseline::getIsDefault, 0)
                    .set(ConfigBaseline::getStatus, 2)  // 归档状态
                    .update();
            }
        } else {
            log.info("没有找到需要归档的旧默认基线（系统[{}]-服务器类型[{}]-配置分类[{}]-基线名称[{}]）", 
                systemId, serverTypeId, categoryId, baselineName);
        }
        
        // 4. 创建新基线
        ConfigBaseline newBaseline = new ConfigBaseline();
        newBaseline.setSystemId(systemId);
        newBaseline.setServerTypeId(serverTypeId);
        newBaseline.setCategoryId(categoryId);
        newBaseline.setBaselineName(baselineName);  // 使用传入的基线名称
        newBaseline.setBaselineVersion(newVersion);
        newBaseline.setFileName(fileName);
        newBaseline.setConfigContent(currentContent);
        newBaseline.setConfigHash(calculateHash(currentContent));
        newBaseline.setIsDefault(1);  // 设为默认
        newBaseline.setStatus(1);  // 生效状态
        newBaseline.setDescription(description);
        newBaseline.setSourceType("PROMOTE");  // 标记为晋级来源
        
        boolean saveSuccess = this.save(newBaseline);
        if (!saveSuccess) {
            throw new RuntimeException("创建新基线失败");
        }
        
        // 由于 BaseEntity 使用 IdType.INPUT，保存后需要重新查询以获取生成的 ID
        ConfigBaseline savedBaseline = getDefaultBaseline(systemId, serverTypeId, categoryId);
        if (savedBaseline == null || savedBaseline.getId() == null) {
            throw new RuntimeException("保存基线成功但无法获取生成的ID");
        }
        
        // 5. 记录版本切换日志
        BaselineVersionLog versionLog = new BaselineVersionLog();
        versionLog.setSystemId(systemId);
        versionLog.setServerTypeId(serverTypeId);
        versionLog.setCategoryId(categoryId);
        versionLog.setOldBaselineId(currentDefault != null ? currentDefault.getId() : null);
        versionLog.setNewBaselineId(savedBaseline.getId());  // 使用重新查询获取的ID
        versionLog.setOldVersion(currentDefault != null ? currentDefault.getBaselineVersion() : null);
        versionLog.setNewVersion(newVersion);
        versionLog.setSwitchReason("晋级采集版本为基线");
        versionLog.setOperationType("SWITCH");
        baselineVersionLogService.recordSwitch(versionLog);
        
        log.info("晋级成功，新基线版本: {}", newVersion);
        return savedBaseline;  // 返回包含ID的基线对象
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean switchToVersion(Long baselineId, String reason) {
        // 1. 获取目标基线
        ConfigBaseline targetBaseline = this.getById(baselineId);
        if (targetBaseline == null) {
            throw new RuntimeException("目标基线不存在");
        }
        
        // 2. 检查是否已是默认版本
        if (targetBaseline.getIsDefault() == 1 && targetBaseline.getStatus() == 1) {
            throw new RuntimeException("该版本已是当前默认版本");
        }
        
        log.info("开始切换版本: 目标版本={} ({})", targetBaseline.getBaselineName(), targetBaseline.getBaselineVersion());
        
        // 3. 归档旧的默认基线
        // 使用 systemId + serverTypeId + categoryId + baselineName 作为唯一标识
        // 只归档生效状态（status=1）的默认基线
        LambdaQueryWrapper<ConfigBaseline> archiveWrapper = new LambdaQueryWrapper<>();
        archiveWrapper.eq(ConfigBaseline::getSystemId, targetBaseline.getSystemId())
                     .eq(ConfigBaseline::getServerTypeId, targetBaseline.getServerTypeId())  // 【关键】精确匹配服务器类型
                     .eq(ConfigBaseline::getCategoryId, targetBaseline.getCategoryId())
                     .eq(ConfigBaseline::getBaselineName, targetBaseline.getBaselineName())  // 【关键】精确匹配基线名称
                     .eq(ConfigBaseline::getIsDefault, 1)
                     .eq(ConfigBaseline::getStatus, 1);  // 只查询生效的基线
        List<ConfigBaseline> oldDefaults = this.list(archiveWrapper);
        
        if (!oldDefaults.isEmpty()) {
            log.info("归档 {} 个旧的生效默认基线（系统[{}]-服务器类型[{}]-配置分类[{}]-基线名称[{}]）", 
                oldDefaults.size(), targetBaseline.getSystemId(), targetBaseline.getServerTypeId(), 
                targetBaseline.getCategoryId(), targetBaseline.getBaselineName());
            for (ConfigBaseline oldDefault : oldDefaults) {
                log.info("  - 归档基线: ID={}, 版本={}", 
                    oldDefault.getId(), oldDefault.getBaselineVersion());
                this.lambdaUpdate()
                    .eq(ConfigBaseline::getId, oldDefault.getId())
                    .set(ConfigBaseline::getIsDefault, 0)
                    .set(ConfigBaseline::getStatus, 2)  // 归档状态
                    .update();
            }
        } else {
            log.info("没有找到需要归档的旧默认基线（系统[{}]-服务器类型[{}]-配置分类[{}]-基线名称[{}]）", 
                targetBaseline.getSystemId(), targetBaseline.getServerTypeId(), 
                targetBaseline.getCategoryId(), targetBaseline.getBaselineName());
        }
        
        ConfigBaseline currentDefault = !oldDefaults.isEmpty() ? oldDefaults.get(0) : null;
        
        // 5. 将目标基线设置为默认
        boolean updateSuccess = this.lambdaUpdate()
            .eq(ConfigBaseline::getId, baselineId)
            .set(ConfigBaseline::getIsDefault, 1)
            .set(ConfigBaseline::getStatus, 1)  // 生效状态
            .update();
            
        if (!updateSuccess) {
            throw new RuntimeException("切换版本失败");
        }
        
        // 6. 记录版本切换日志
        BaselineVersionLog versionLog = new BaselineVersionLog();
        versionLog.setSystemId(targetBaseline.getSystemId());
        versionLog.setServerTypeId(targetBaseline.getServerTypeId());
        versionLog.setCategoryId(targetBaseline.getCategoryId());
        versionLog.setOldBaselineId(currentDefault != null ? currentDefault.getId() : null);
        versionLog.setNewBaselineId(baselineId);
        versionLog.setOldVersion(currentDefault != null ? currentDefault.getBaselineVersion() : null);
        versionLog.setNewVersion(targetBaseline.getBaselineVersion());
        versionLog.setSwitchReason(reason != null ? reason : "切换到历史版本");
        versionLog.setOperationType("ROLLBACK");
        baselineVersionLogService.recordSwitch(versionLog);
        
        log.info("版本切换成功: {} -> {}", 
            currentDefault != null ? currentDefault.getBaselineVersion() : "无",
            targetBaseline.getBaselineVersion());
        
        return true;
    }
    
    /**
     * 计算配置内容的哈希值
     */
    private String calculateHash(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("计算哈希值失败", e);
            return "";
        }
    }
}