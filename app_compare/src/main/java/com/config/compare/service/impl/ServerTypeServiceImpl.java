package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ServerType;
import com.config.compare.mapper.ServerTypeMapper;
import com.config.compare.service.ServerTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 服务器类型Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class ServerTypeServiceImpl extends ServiceImpl<ServerTypeMapper, ServerType> implements ServerTypeService {

    @Override
    public IPage<ServerType> pageQuery(PageRequest pageRequest) {
        Page<ServerType> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ServerType> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(pageRequest.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(ServerType::getTypeName, pageRequest.getKeyword())
                .or()
                .like(ServerType::getTypeCode, pageRequest.getKeyword())
                .or()
                .like(ServerType::getDescription, pageRequest.getKeyword())
            );
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getOrderBy())) {
            if ("ASC".equalsIgnoreCase(pageRequest.getOrderDirection())) {
                switch (pageRequest.getOrderBy()) {
                    case "typeName":
                        queryWrapper.orderByAsc(ServerType::getTypeName);
                        break;
                    case "typeCode":
                        queryWrapper.orderByAsc(ServerType::getTypeCode);
                        break;
                    case "createTime":
                        queryWrapper.orderByAsc(ServerType::getCreateTime);
                        break;
                    case "updateTime":
                        queryWrapper.orderByAsc(ServerType::getUpdateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(ServerType::getCreateTime);
                }
            } else {
                switch (pageRequest.getOrderBy()) {
                    case "typeName":
                        queryWrapper.orderByDesc(ServerType::getTypeName);
                        break;
                    case "typeCode":
                        queryWrapper.orderByDesc(ServerType::getTypeCode);
                        break;
                    case "createTime":
                        queryWrapper.orderByDesc(ServerType::getCreateTime);
                        break;
                    case "updateTime":
                        queryWrapper.orderByDesc(ServerType::getUpdateTime);
                        break;
                    default:
                        queryWrapper.orderByDesc(ServerType::getCreateTime);
                }
            }
        } else {
            queryWrapper.orderByDesc(ServerType::getCreateTime);
        }
        
        return this.page(page, queryWrapper);
    }

    @Override
    public ServerType getByTypeCode(String typeCode) {
        LambdaQueryWrapper<ServerType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerType::getTypeCode, typeCode);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean checkTypeCodeExists(String typeCode, Long excludeId) {
        LambdaQueryWrapper<ServerType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerType::getTypeCode, typeCode);
        if (excludeId != null) {
            queryWrapper.ne(ServerType::getId, excludeId);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public List<ServerType> listEnabled() {
        LambdaQueryWrapper<ServerType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerType::getStatus, 1);
        queryWrapper.orderByAsc(ServerType::getTypeName);
        return this.list(queryWrapper);
    }

    @Override
    public List<ServerType> listBySystemId(Long systemId) {
        // 服务器类型是全局的，不按系统过滤，但可以根据该系统下是否有服务器实例来过滤
        // 为了简化，这里先返回所有启用的服务器类型
        return listEnabled();
    }

    @Override
    public boolean createServerType(ServerType serverType) {
        // 检查类型编码是否已存在
        if (checkTypeCodeExists(serverType.getTypeCode(), null)) {
            throw new RuntimeException("服务器类型编码已存在");
        }
        
        return this.save(serverType);
    }

    @Override
    public boolean updateServerType(ServerType serverType) {
        // 检查类型编码是否已存在
        if (checkTypeCodeExists(serverType.getTypeCode(), serverType.getId())) {
            throw new RuntimeException("服务器类型编码已存在");
        }
        
        return this.updateById(serverType);
    }

    @Override
    public boolean deleteServerType(Long id) {
        // 这里可以添加删除前的检查逻辑，比如检查是否有关联的服务器实例
        // 暂时直接删除
        return this.removeById(id);
    }


}