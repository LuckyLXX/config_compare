package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ServerInstance;
import com.config.compare.entity.ServerType;
import com.config.compare.entity.SystemInfo;
import com.config.compare.mapper.ServerInstanceMapper;
import com.config.compare.service.ServerInstanceService;
import com.config.compare.service.ServerTypeService;
import com.config.compare.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 服务器实例Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServerInstanceServiceImpl extends ServiceImpl<ServerInstanceMapper, ServerInstance> implements ServerInstanceService {

    private final SystemInfoService systemInfoService;
    private final ServerTypeService serverTypeService;

    @Override
    public IPage<ServerInstance> pageQuery(PageRequest pageRequest) {
        Page<ServerInstance> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ServerInstance> queryWrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索
        if (StringUtils.hasText(pageRequest.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                .like(ServerInstance::getInstanceName, pageRequest.getKeyword())
                .or()
                .like(ServerInstance::getServerIp, pageRequest.getKeyword())
                .or()
                .like(ServerInstance::getDescription, pageRequest.getKeyword())
            );
        }
        
        // 排序
        if (StringUtils.hasText(pageRequest.getOrderBy())) {
            if ("ASC".equalsIgnoreCase(pageRequest.getOrderDirection())) {
                switch (pageRequest.getOrderBy()) {
                    case "instanceName":
                        queryWrapper.orderByAsc(ServerInstance::getInstanceName);
                        break;
                    case "serverIp":
                        queryWrapper.orderByAsc(ServerInstance::getServerIp);
                        break;
                    case "connectStatus":
                        queryWrapper.orderByAsc(ServerInstance::getConnectStatus);
                        break;
                    case "createTime":
                        queryWrapper.orderByAsc(ServerInstance::getCreateTime);
                        break;
                    case "updateTime":
                        queryWrapper.orderByAsc(ServerInstance::getUpdateTime);
                        break;
                    default:
                        queryWrapper.orderByAsc(ServerInstance::getCreateTime);
                }
            } else {
                switch (pageRequest.getOrderBy()) {
                    case "instanceName":
                        queryWrapper.orderByDesc(ServerInstance::getInstanceName);
                        break;
                    case "serverIp":
                        queryWrapper.orderByDesc(ServerInstance::getServerIp);
                        break;
                    case "connectStatus":
                        queryWrapper.orderByDesc(ServerInstance::getConnectStatus);
                        break;
                    case "createTime":
                        queryWrapper.orderByDesc(ServerInstance::getCreateTime);
                        break;
                    case "updateTime":
                        queryWrapper.orderByDesc(ServerInstance::getUpdateTime);
                        break;
                    default:
                        queryWrapper.orderByDesc(ServerInstance::getCreateTime);
                }
            }
        } else {
            queryWrapper.orderByDesc(ServerInstance::getCreateTime);
        }
        
        return this.page(page, queryWrapper);
    }

    @Override
    public IPage<Map<String, Object>> pageQueryWithDetails(PageRequest pageRequest) {
        // 先获取基础分页数据
        IPage<ServerInstance> instancePage = pageQuery(pageRequest);
        
        // 转换为包含详细信息的Map
        Page<Map<String, Object>> resultPage = new Page<>(instancePage.getCurrent(), instancePage.getSize());
        resultPage.setTotal(instancePage.getTotal());
        
        List<Map<String, Object>> records = instancePage.getRecords().stream().map(instance -> {
            Map<String, Object> record = new HashMap<>();
            
            // 复制基础字段
            record.put("id", instance.getId());
            record.put("systemId", instance.getSystemId());
            record.put("serverTypeId", instance.getServerTypeId());
            record.put("instanceName", instance.getInstanceName());
            record.put("serverIp", instance.getServerIp());
            record.put("sshPort", instance.getSshPort());
            record.put("username", instance.getUsername());
            record.put("password", instance.getPassword());
            record.put("serverRole", instance.getServerRole());
            record.put("apolloServerUrl", instance.getApolloServerUrl());
            record.put("apolloAppId", instance.getApolloAppId());
            record.put("apolloCluster", instance.getApolloCluster());
            record.put("apolloEnv", instance.getApolloEnv());
            record.put("apolloNamespaces", instance.getApolloNamespaces());
            record.put("apolloToken", instance.getApolloToken());
            record.put("customConfig", instance.getCustomConfig());
            record.put("status", instance.getStatus());
            record.put("lastConnectTime", instance.getLastConnectTime());
            record.put("connectStatus", instance.getConnectStatus());
            record.put("description", instance.getDescription());
            record.put("createTime", instance.getCreateTime());
            record.put("updateTime", instance.getUpdateTime());
            record.put("createBy", instance.getCreateBy());
            record.put("updateBy", instance.getUpdateBy());
            
            // 获取关联信息
            try {
                // 获取系统信息
                SystemInfo systemInfo = systemInfoService.getById(instance.getSystemId());
                record.put("systemName", systemInfo != null ? systemInfo.getSystemName() : "未知系统");
                
                // 获取服务器类型信息
                ServerType serverType = serverTypeService.getById(instance.getServerTypeId());
                record.put("serverTypeName", serverType != null ? serverType.getTypeName() : "未知类型");
            } catch (Exception e) {
                log.warn("获取关联信息失败: instanceId={}", instance.getId(), e);
                record.put("systemName", "未知系统");
                record.put("serverTypeName", "未知类型");
            }
            
            return record;
        }).collect(Collectors.toList());
        
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public List<ServerInstance> listBySystemAndType(Long systemId, Long serverTypeId) {
        LambdaQueryWrapper<ServerInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerInstance::getSystemId, systemId)
                   .eq(ServerInstance::getServerTypeId, serverTypeId)
                   .eq(ServerInstance::getStatus, 1)
                   .orderByAsc(ServerInstance::getInstanceName);
        return this.list(queryWrapper);
    }

    @Override
    public List<ServerInstance> listBySystemId(Long systemId) {
        LambdaQueryWrapper<ServerInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerInstance::getSystemId, systemId)
                   .eq(ServerInstance::getStatus, 1)
                   .orderByAsc(ServerInstance::getInstanceName);
        return this.list(queryWrapper);
    }

    @Override
    public List<ServerInstance> listBySystemAndTypes(Long systemId, String[] serverTypeIds) {
        LambdaQueryWrapper<ServerInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerInstance::getSystemId, systemId)
                   .eq(ServerInstance::getStatus, 1);
        
        if (serverTypeIds != null && serverTypeIds.length > 0) {
            // 将字符串数组转换为Long数组
            List<Long> typeIds = new ArrayList<>();
            for (String serverTypeId : serverTypeIds) {
                try {
                    typeIds.add(Long.parseLong(serverTypeId.trim()));
                } catch (NumberFormatException e) {
                    log.warn("无效的服务器类型ID: {}", serverTypeId);
                }
            }
            if (!typeIds.isEmpty()) {
                queryWrapper.in(ServerInstance::getServerTypeId, typeIds);
            }
        }
        
        queryWrapper.orderByAsc(ServerInstance::getInstanceName);
        return this.list(queryWrapper);
    }

    @Override
    public boolean checkInstanceNameExists(Long systemId, Long serverTypeId, String instanceName, Long excludeId) {
        LambdaQueryWrapper<ServerInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ServerInstance::getSystemId, systemId)
                   .eq(ServerInstance::getServerTypeId, serverTypeId)
                   .eq(ServerInstance::getInstanceName, instanceName);
        if (excludeId != null) {
            queryWrapper.ne(ServerInstance::getId, excludeId);
        }
        return this.count(queryWrapper) > 0;
    }

    @Override
    public boolean createInstance(ServerInstance serverInstance) {
        // 检查实例名称是否已存在
        if (checkInstanceNameExists(serverInstance.getSystemId(), 
                                  serverInstance.getServerTypeId(),
                                  serverInstance.getInstanceName(), 
                                  null)) {
            throw new RuntimeException("该系统下的服务器类型中已存在相同实例名称");
        }
        
        // 设置默认值
        if (serverInstance.getSshPort() == null) {
            serverInstance.setSshPort(22);
        }
        if (serverInstance.getConnectStatus() == null) {
            serverInstance.setConnectStatus(0);
        }
        
        return this.save(serverInstance);
    }

    @Override
    public boolean updateInstance(ServerInstance serverInstance) {
        // 检查实例名称是否已存在
        if (checkInstanceNameExists(serverInstance.getSystemId(),
                                  serverInstance.getServerTypeId(),
                                  serverInstance.getInstanceName(),
                                  serverInstance.getId())) {
            throw new RuntimeException("该系统下的服务器类型中已存在相同实例名称");
        }
        
        return this.updateById(serverInstance);
    }

    @Override
    public boolean deleteInstance(Long id) {
        // 这里可以添加删除前的检查逻辑，比如检查是否有关联的采集任务、基线等
        // 暂时直接删除
        return this.removeById(id);
    }

    @Override
    public boolean testConnection(Long id) {
        ServerInstance instance = this.getById(id);
        if (instance == null) {
            throw new RuntimeException("服务器实例不存在");
        }
        
        boolean connectResult = false;
        String errorMessage = null;
        
        try {
            if (StringUtils.hasText(instance.getServerIp())) {
                // SSH/SFTP连接测试
                log.info("开始SSH连接测试: {}@{}:{}", instance.getUsername(), instance.getServerIp(), instance.getSshPort());
                connectResult = com.config.compare.util.SshConnectionUtil.testSshConnectionWithRetry(
                    instance.getServerIp(), 
                    instance.getSshPort(), 
                    instance.getUsername(), 
                    instance.getPassword(),
                    2 // 重试2次
                );
                
                if (!connectResult) {
                    errorMessage = "SSH连接失败，请检查服务器地址、端口、用户名和密码";
                }
                
            } else if (StringUtils.hasText(instance.getApolloServerUrl())) {
                // Apollo配置中心连接测试
                log.info("开始Apollo连接测试: {}", instance.getApolloServerUrl());
                connectResult = com.config.compare.util.HttpConnectionUtil.testApolloConnection(
                    instance.getApolloServerUrl(),
                    instance.getApolloAppId()
                );
                
                if (!connectResult) {
                    errorMessage = "Apollo连接失败，请检查服务器地址和网络连接";
                }
                
            } else {
                errorMessage = "服务器实例配置不完整，缺少连接信息";
                log.warn("服务器实例配置不完整: ID={}, serverIp={}, apolloServerUrl={}", 
                    id, instance.getServerIp(), instance.getApolloServerUrl());
            }
            
            // 更新连接状态和连接时间
            updateConnectStatus(id, connectResult ? 1 : 0);
            this.lambdaUpdate()
                .eq(ServerInstance::getId, id)
                .set(ServerInstance::getLastConnectTime, LocalDateTime.now())
                .update();
                
            log.info("连接测试完成: ID={}, 结果={}", id, connectResult ? "成功" : "失败");
                
        } catch (Exception e) {
            log.error("连接测试异常: ID={}", id, e);
            errorMessage = "连接测试异常: " + e.getMessage();
            connectResult = false;
            updateConnectStatus(id, 0);
        }
        
        // 如果连接失败，抛出异常以便前端显示具体错误信息
        if (!connectResult && StringUtils.hasText(errorMessage)) {
            throw new RuntimeException(errorMessage);
        }
        
        return connectResult;
    }

    @Override
    public void updateConnectStatus(Long id, Integer connectStatus) {
        this.lambdaUpdate()
            .eq(ServerInstance::getId, id)
            .set(ServerInstance::getConnectStatus, connectStatus)
            .update();
    }
}