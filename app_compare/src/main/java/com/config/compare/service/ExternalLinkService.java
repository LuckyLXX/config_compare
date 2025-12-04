package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ExternalLink;

import java.util.List;

/**
 * 外部链接Service接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-28
 */
public interface ExternalLinkService extends IService<ExternalLink> {

    /**
     * 分页查询外部链接
     * 
     * @param pageRequest 分页请求参数
     * @param linkName 链接名称
     * @param openType 打开方式
     * @param status 状态
     * @return 分页结果
     */
    IPage<ExternalLink> pageQuery(PageRequest pageRequest, String linkName, Integer openType, Integer status);

    /**
     * 获取所有启用的外部链接
     * 
     * @return 外部链接列表
     */
    List<ExternalLink> getEnabledLinks();

    /**
     * 检查链接名称是否存在
     * 
     * @param linkName 链接名称
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean checkLinkNameExists(String linkName, Long excludeId);

    /**
     * 创建外部链接
     * 
     * @param externalLink 外部链接
     * @return 创建结果
     */
    boolean createLink(ExternalLink externalLink);

    /**
     * 更新外部链接
     * 
     * @param externalLink 外部链接
     * @return 更新结果
     */
    boolean updateLink(ExternalLink externalLink);

    /**
     * 删除外部链接
     * 
     * @param id 链接ID
     * @return 删除结果
     */
    boolean deleteLink(Long id);

    /**
     * 批量删除外部链接
     * 
     * @param ids 链接ID列表
     * @return 删除结果
     */
    boolean batchDeleteLinks(List<Long> ids);

    /**
     * 更新链接状态
     * 
     * @param id 链接ID
     * @param status 状态
     * @return 更新结果
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 更新排序
     * 
     * @param id 链接ID
     * @param sortOrder 排序值
     * @return 更新结果
     */
    boolean updateSortOrder(Long id, Integer sortOrder);
}
