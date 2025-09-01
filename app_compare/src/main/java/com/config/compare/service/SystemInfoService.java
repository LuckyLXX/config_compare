package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.SystemInfo;

/**
 * 系统信息Service接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface SystemInfoService extends IService<SystemInfo> {

    /**
     * 分页查询系统信息
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果
     */
    IPage<SystemInfo> pageQuery(PageRequest pageRequest);

    /**
     * 根据系统名称查询系统信息
     * 
     * @param systemName 系统名称
     * @return 系统信息
     */
    SystemInfo getBySystemName(String systemName);

    /**
     * 检查系统名称是否存在
     * 
     * @param systemName 系统名称
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkSystemNameExists(String systemName, Long excludeId);

    /**
     * 创建系统信息
     * 
     * @param systemInfo 系统信息
     * @return 创建结果
     */
    boolean createSystem(SystemInfo systemInfo);

    /**
     * 更新系统信息
     * 
     * @param systemInfo 系统信息
     * @return 更新结果
     */
    boolean updateSystem(SystemInfo systemInfo);

    /**
     * 删除系统信息
     * 
     * @param id 系统ID
     * @return 删除结果
     */
    boolean deleteSystem(Long id);
}