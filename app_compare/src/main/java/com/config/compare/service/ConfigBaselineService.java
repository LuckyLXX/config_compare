package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ConfigBaseline;

import java.util.List;

/**
 * 配置基线Service
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface ConfigBaselineService extends IService<ConfigBaseline> {

    /**
     * 分页查询配置基线
     * 
     * @param pageRequest 分页请求参数
     * @return 分页结果
     */
    IPage<ConfigBaseline> pageQuery(PageRequest pageRequest);

    /**
     * 根据系统ID、服务器类型ID和配置分类ID查询基线列表
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @return 基线列表
     */
    List<ConfigBaseline> listBySystemTypeCategory(Long systemId, Long serverTypeId, Long categoryId);

    /**
     * 获取默认基线
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @return 默认基线
     */
    ConfigBaseline getDefaultBaseline(Long systemId, Long serverTypeId, Long categoryId);

    /**
     * 检查基线名称是否存在
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @param baselineName 基线名称
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkBaselineNameExists(Long systemId, Long serverTypeId, Long categoryId, 
                                   String baselineName, Long excludeId);

    /**
     * 创建配置基线
     * 
     * @param configBaseline 配置基线
     * @return 创建结果
     */
    boolean createBaseline(ConfigBaseline configBaseline);

    /**
     * 更新配置基线
     * 
     * @param configBaseline 配置基线
     * @return 更新结果
     */
    boolean updateBaseline(ConfigBaseline configBaseline);

    /**
     * 删除配置基线
     * 
     * @param id 基线ID
     * @return 删除结果
     */
    boolean deleteBaseline(Long id);

    /**
     * 设置默认基线
     * 
     * @param id 基线ID
     * @param reason 切换原因
     * @return 设置结果
     */
    boolean setDefaultBaseline(Long id, String reason);

    /**
     * 复制基线
     * 
     * @param sourceId 源基线ID
     * @param newName 新基线名称
     * @param newVersion 新版本号
     * @param description 描述
     * @return 复制结果
     */
    boolean copyBaseline(Long sourceId, String newName, String newVersion, String description);

    /**
     * 比较两个基线的差异
     * 
     * @param baseline1Id 基线1 ID
     * @param baseline2Id 基线2 ID
     * @return 差异信息
     */
    String compareBaselines(Long baseline1Id, Long baseline2Id);

    /**
     * 获取基线版本历史
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @param categoryId 配置分类ID
     * @return 版本历史
     */
    List<ConfigBaseline> getVersionHistory(Long systemId, Long serverTypeId, Long categoryId);
}