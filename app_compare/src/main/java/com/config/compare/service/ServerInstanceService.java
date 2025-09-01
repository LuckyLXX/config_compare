package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ServerInstance;

import java.util.List;
import java.util.Map;

/**
 * 服务器实例Service接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ServerInstanceService extends IService<ServerInstance> {

    /**
     * 分页查询服务器实例
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果
     */
    IPage<ServerInstance> pageQuery(PageRequest pageRequest);

    /**
     * 分页查询服务器实例（包含关联信息）
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果，包含系统名称和服务器类型名称
     */
    IPage<Map<String, Object>> pageQueryWithDetails(PageRequest pageRequest);

    /**
     * 根据系统ID和服务器类型ID查询实例列表
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @return 实例列表
     */
    List<ServerInstance> listBySystemAndType(Long systemId, Long serverTypeId);

    /**
     * 根据系统ID查询实例列表
     * 
     * @param systemId 系统ID
     * @return 实例列表
     */
    List<ServerInstance> listBySystemId(Long systemId);

    /**
     * 根据系统ID和多个服务器类型ID查询实例列表
     * 
     * @param systemId 系统ID
     * @param serverTypeIds 服务器类型ID数组
     * @return 实例列表
     */
    List<ServerInstance> listBySystemAndTypes(Long systemId, String[] serverTypeIds);

    /**
     * 检查实例名称是否存在
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param instanceName 实例名称
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkInstanceNameExists(Long systemId, Long serverTypeId, String instanceName, Long excludeId);

    /**
     * 创建服务器实例
     * 
     * @param serverInstance 服务器实例
     * @return 创建结果
     */
    boolean createInstance(ServerInstance serverInstance);

    /**
     * 更新服务器实例
     * 
     * @param serverInstance 服务器实例
     * @return 更新结果
     */
    boolean updateInstance(ServerInstance serverInstance);

    /**
     * 删除服务器实例
     * 
     * @param id 实例ID
     * @return 删除结果
     */
    boolean deleteInstance(Long id);

    /**
     * 测试连接
     * 
     * @param id 实例ID
     * @return 连接测试结果
     */
    boolean testConnection(Long id);

    /**
     * 更新连接状态
     * 
     * @param id 实例ID
     * @param connectStatus 连接状态
     */
    void updateConnectStatus(Long id, Integer connectStatus);
}