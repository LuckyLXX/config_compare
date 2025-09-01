package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.SystemInfo;
import com.config.compare.mapper.SystemInfoMapper;
import com.config.compare.service.SystemInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 系统信息Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class SystemInfoServiceImpl extends ServiceImpl<SystemInfoMapper, SystemInfo> implements SystemInfoService {

    @Override
    public IPage<SystemInfo> pageQuery(PageRequest pageRequest) {
        Page<SystemInfo> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<SystemInfo> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(pageRequest.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(SystemInfo::getSystemName, pageRequest.getKeyword())
                .or()
                .like(SystemInfo::getSystemDesc, pageRequest.getKeyword())
                .or()
                .like(SystemInfo::getOwner, pageRequest.getKeyword())
            );
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getOrderBy())) {
            if ("ASC".equalsIgnoreCase(pageRequest.getOrderDirection())) {
                switch (pageRequest.getOrderBy()) {
                    case "systemName":
                        queryWrapper.orderByAsc(SystemInfo::getSystemName);
                        break;
                    case "envType":
                        queryWrapper.orderByAsc(SystemInfo::getEnvType);
                        break;
                    case "createTime":
                        queryWrapper.orderByAsc(SystemInfo::getCreateTime);
                        break;
                    case "updateTime":
                        queryWrapper.orderByAsc(SystemInfo::getUpdateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(SystemInfo::getCreateTime);
                }
            } else {
                switch (pageRequest.getOrderBy()) {
                    case "systemName":
                        queryWrapper.orderByDesc(SystemInfo::getSystemName);
                        break;
                    case "envType":
                        queryWrapper.orderByDesc(SystemInfo::getEnvType);
                        break;
                    case "createTime":
                        queryWrapper.orderByDesc(SystemInfo::getCreateTime);
                        break;
                    case "updateTime":
                        queryWrapper.orderByDesc(SystemInfo::getUpdateTime);
                        break;
                    default:
                        queryWrapper.orderByDesc(SystemInfo::getCreateTime);
                }
            }
        } else {
            queryWrapper.orderByDesc(SystemInfo::getCreateTime);
        }
        
        return this.page(page, queryWrapper);
    }

    @Override
    public SystemInfo getBySystemName(String systemName) {
        LambdaQueryWrapper<SystemInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemInfo::getSystemName, systemName);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean checkSystemNameExists(String systemName, Long excludeId) {
        LambdaQueryWrapper<SystemInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemInfo::getSystemName, systemName);
        if (excludeId != null) {
            queryWrapper.ne(SystemInfo::getId, excludeId);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean createSystem(SystemInfo systemInfo) {
        // 检查系统名称是否已存在
        if (checkSystemNameExists(systemInfo.getSystemName(), null)) {
            throw new RuntimeException("系统名称已存在");
        }
        
        return this.save(systemInfo);
    }

    @Override
    public boolean updateSystem(SystemInfo systemInfo) {
        // 检查系统名称是否已存在
        if (checkSystemNameExists(systemInfo.getSystemName(), systemInfo.getId())) {
            throw new RuntimeException("系统名称已存在");
        }
        
        return this.updateById(systemInfo);
    }

    @Override
    public boolean deleteSystem(Long id) {
        // 这里可以添加删除前的检查逻辑，比如检查是否有关联的服务器实例、基线等
        // 暂时直接删除
        return this.removeById(id);
    }


}