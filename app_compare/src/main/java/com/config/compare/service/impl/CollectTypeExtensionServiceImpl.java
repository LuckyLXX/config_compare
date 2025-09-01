package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.collect.handler.CollectHandler;
import com.config.compare.collect.manager.CollectHandlerManager;
import com.config.compare.collect.model.CollectContext;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.CollectTypeExtension;
import com.config.compare.entity.ServerInstance;
import com.config.compare.mapper.CollectTypeExtensionMapper;
import com.config.compare.service.CollectTypeExtensionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集类型扩展Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectTypeExtensionServiceImpl extends ServiceImpl<CollectTypeExtensionMapper, CollectTypeExtension> implements CollectTypeExtensionService {

    private final CollectHandlerManager handlerManager;

    @Override
    public IPage<CollectTypeExtension> pageQuery(PageRequest pageRequest) {
        Page<CollectTypeExtension> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<CollectTypeExtension> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(pageRequest.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(CollectTypeExtension::getTypeName, pageRequest.getKeyword())
                .or()
                .like(CollectTypeExtension::getTypeCode, pageRequest.getKeyword())
                .or()
                .like(CollectTypeExtension::getDescription, pageRequest.getKeyword())
            );
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getOrderBy())) {
            if ("ASC".equalsIgnoreCase(pageRequest.getOrderDirection())) {
                switch (pageRequest.getOrderBy()) {
                    case "typeName":
                        queryWrapper.orderByAsc(CollectTypeExtension::getTypeName);
                        break;
                    case "typeCode":
                        queryWrapper.orderByAsc(CollectTypeExtension::getTypeCode);
                        break;
                    case "typeCategory":
                        queryWrapper.orderByAsc(CollectTypeExtension::getTypeCategory);
                        break;
                    case "createTime":
                        queryWrapper.orderByAsc(CollectTypeExtension::getCreateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(CollectTypeExtension::getTypeCategory)
                                   .orderByAsc(CollectTypeExtension::getTypeName);
                }
            } else {
                switch (pageRequest.getOrderBy()) {
                    case "typeName":
                        queryWrapper.orderByDesc(CollectTypeExtension::getTypeName);
                        break;
                    case "typeCode":
                        queryWrapper.orderByDesc(CollectTypeExtension::getTypeCode);
                        break;
                    case "typeCategory":
                        queryWrapper.orderByDesc(CollectTypeExtension::getTypeCategory);
                        break;
                    case "createTime":
                        queryWrapper.orderByDesc(CollectTypeExtension::getCreateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(CollectTypeExtension::getTypeCategory)
                                   .orderByDesc(CollectTypeExtension::getCreateTime);
                }
            }
        } else {
            queryWrapper.orderByAsc(CollectTypeExtension::getTypeCategory)
                       .orderByAsc(CollectTypeExtension::getTypeName);
        }
        
        return this.page(page, queryWrapper);
    }

    @Override
    public List<CollectTypeExtension> listByTypeCategory(String typeCategory) {
        return baseMapper.selectByTypeCategory(typeCategory);
    }

    @Override
    public List<CollectTypeExtension> listEnabledTypes() {
        return baseMapper.selectEnabledTypes();
    }

    @Override
    public CollectTypeExtension getByTypeCode(String typeCode) {
        return baseMapper.selectByTypeCode(typeCode);
    }

    @Override
    public boolean checkTypeCodeExists(String typeCode, Long excludeId) {
        LambdaQueryWrapper<CollectTypeExtension> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectTypeExtension::getTypeCode, typeCode);
        if (excludeId != null) {
            queryWrapper.ne(CollectTypeExtension::getId, excludeId);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean createTypeExtension(CollectTypeExtension collectTypeExtension) {
        // 检查类型编码是否已存在
        if (checkTypeCodeExists(collectTypeExtension.getTypeCode(), null)) {
            throw new RuntimeException("类型编码已存在");
        }
        
        // 验证处理器类
        if (!validateHandlerClass(collectTypeExtension.getHandlerClass())) {
            throw new RuntimeException("处理器类无效或不存在");
        }
        
        // 设置默认值
        if (collectTypeExtension.getStatus() == null) {
            collectTypeExtension.setStatus(1);
        }
        
        return this.save(collectTypeExtension);
    }

    @Override
    public boolean updateTypeExtension(CollectTypeExtension collectTypeExtension) {
        // 检查类型编码是否已存在
        if (checkTypeCodeExists(collectTypeExtension.getTypeCode(), collectTypeExtension.getId())) {
            throw new RuntimeException("类型编码已存在");
        }
        
        // 验证处理器类
        if (!validateHandlerClass(collectTypeExtension.getHandlerClass())) {
            throw new RuntimeException("处理器类无效或不存在");
        }
        
        return this.updateById(collectTypeExtension);
    }

    @Override
    public boolean deleteTypeExtension(Long id) {
        // TODO: 检查是否被采集模板或任务使用
        return this.removeById(id);
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        return this.lambdaUpdate()
            .eq(CollectTypeExtension::getId, id)
            .set(CollectTypeExtension::getStatus, status)
            .update();
    }

    @Override
    public boolean syncHandlersToDatabase() {
        try {
            Collection<CollectHandler> handlers = handlerManager.getAllHandlers();
            
            for (CollectHandler handler : handlers) {
                String typeCode = handler.getTypeCode();
                CollectTypeExtension existing = getByTypeCode(typeCode);
                
                if (existing == null) {
                    // 创建新的采集类型
                    CollectTypeExtension typeExtension = new CollectTypeExtension();
                    typeExtension.setTypeCode(typeCode);
                    typeExtension.setTypeName(handler.getTypeName());
                    typeExtension.setTypeCategory("BASIC"); // 默认为基础类型
                    typeExtension.setHandlerClass(handler.getClass().getName());
                    typeExtension.setConfigSchema(handler.getConfigSchema());
                    typeExtension.setDescription(handler.getDescription());
                    typeExtension.setStatus(1);
                    
                    this.save(typeExtension);
                    log.info("同步新增采集类型：{}", typeCode);
                } else {
                    // 更新现有的采集类型信息
                    existing.setTypeName(handler.getTypeName());
                    existing.setHandlerClass(handler.getClass().getName());
                    existing.setConfigSchema(handler.getConfigSchema());
                    existing.setDescription(handler.getDescription());
                    
                    this.updateById(existing);
                    log.info("同步更新采集类型：{}", typeCode);
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("同步处理器信息失败", e);
            return false;
        }
    }

    @Override
    public CollectHandler getHandler(String typeCode) {
        return handlerManager.getHandler(typeCode);
    }

    @Override
    public boolean validateHandlerClass(String handlerClass) {
        if (!StringUtils.hasText(handlerClass)) {
            return false;
        }
        
        try {
            Class<?> clazz = Class.forName(handlerClass);
            return CollectHandler.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            log.warn("处理器类不存在：{}", handlerClass);
            return false;
        }
    }

    @Override
    public boolean testTypeConnection(String typeCode, String config) {
        CollectHandler handler = getHandler(typeCode);
        if (handler == null) {
            log.error("找不到采集类型处理器：{}", typeCode);
            return false;
        }
        
        try {
            // 创建测试上下文
            CollectContext context = new CollectContext();
            context.setCollectType(typeCode);
            context.setCollectItemName("CONNECTION_TEST");
            
            // 解析配置参数
            if (StringUtils.hasText(config)) {
                // TODO: 解析JSON配置到configParams
                Map<String, Object> configParams = new HashMap<>();
                context.setConfigParams(configParams);
            }
            
            // 创建测试服务器实例
            ServerInstance testServer = new ServerInstance();
            // TODO: 从配置中解析服务器连接信息
            context.setServerInstance(testServer);
            
            return handler.testConnection(context);
            
        } catch (Exception e) {
            log.error("测试采集类型连接失败", e);
            return false;
        }
    }
}