package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ServerType;

import java.util.List;

/**
 * 服务器类型Service接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ServerTypeService extends IService<ServerType> {

    /**
     * 分页查询服务器类型
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果
     */
    IPage<ServerType> pageQuery(PageRequest pageRequest);

    /**
     * 根据类型编码查询服务器类型
     * 
     * @param typeCode 类型编码
     * @return 服务器类型
     */
    ServerType getByTypeCode(String typeCode);

    /**
     * 检查类型编码是否存在
     * 
     * @param typeCode 类型编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkTypeCodeExists(String typeCode, Long excludeId);

    /**
     * 获取所有启用的服务器类型
     * 
     * @return 服务器类型列表
     */
    List<ServerType> listEnabled();

    /**
     * 根据系统ID获取服务器类型列表
     * 
     * @param systemId 系统ID
     * @return 服务器类型列表
     */
    List<ServerType> listBySystemId(Long systemId);

    /**
     * 创建服务器类型
     * 
     * @param serverType 服务器类型
     * @return 创建结果
     */
    boolean createServerType(ServerType serverType);

    /**
     * 更新服务器类型
     * 
     * @param serverType 服务器类型
     * @return 更新结果
     */
    boolean updateServerType(ServerType serverType);

    /**
     * 删除服务器类型
     * 
     * @param id 服务器类型ID
     * @return 删除结果
     */
    boolean deleteServerType(Long id);
}