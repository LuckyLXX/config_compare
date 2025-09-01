package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.collect.handler.CollectHandler;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.CollectTypeExtension;

import java.util.List;

/**
 * 采集类型扩展Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CollectTypeExtensionService extends IService<CollectTypeExtension> {

    /**
     * 分页查询采集类型扩展
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果
     */
    IPage<CollectTypeExtension> pageQuery(PageRequest pageRequest);

    /**
     * 根据类型分类查询采集类型列表
     * 
     * @param typeCategory 类型分类
     * @return 采集类型列表
     */
    List<CollectTypeExtension> listByTypeCategory(String typeCategory);

    /**
     * 获取所有启用的采集类型
     * 
     * @return 采集类型列表
     */
    List<CollectTypeExtension> listEnabledTypes();

    /**
     * 根据类型编码获取采集类型
     * 
     * @param typeCode 类型编码
     * @return 采集类型
     */
    CollectTypeExtension getByTypeCode(String typeCode);

    /**
     * 检查类型编码是否存在
     * 
     * @param typeCode 类型编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkTypeCodeExists(String typeCode, Long excludeId);

    /**
     * 创建采集类型扩展
     * 
     * @param collectTypeExtension 采集类型扩展
     * @return 创建结果
     */
    boolean createTypeExtension(CollectTypeExtension collectTypeExtension);

    /**
     * 更新采集类型扩展
     * 
     * @param collectTypeExtension 采集类型扩展
     * @return 更新结果
     */
    boolean updateTypeExtension(CollectTypeExtension collectTypeExtension);

    /**
     * 删除采集类型扩展
     * 
     * @param id 类型ID
     * @return 删除结果
     */
    boolean deleteTypeExtension(Long id);

    /**
     * 启用/禁用采集类型
     * 
     * @param id 类型ID
     * @param status 状态（1启用，0禁用）
     * @return 操作结果
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 同步处理器信息到数据库
     * 
     * @return 同步结果
     */
    boolean syncHandlersToDatabase();

    /**
     * 获取处理器实例
     * 
     * @param typeCode 类型编码
     * @return 处理器实例
     */
    CollectHandler getHandler(String typeCode);

    /**
     * 验证处理器类是否有效
     * 
     * @param handlerClass 处理器类名
     * @return 是否有效
     */
    boolean validateHandlerClass(String handlerClass);

    /**
     * 测试采集类型的连接
     * 
     * @param typeCode 类型编码
     * @param config 配置参数
     * @return 测试结果
     */
    boolean testTypeConnection(String typeCode, String config);
}