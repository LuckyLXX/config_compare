package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.common.request.PageRequest;
import com.config.compare.entity.ExternalLink;
import com.config.compare.mapper.ExternalLinkMapper;
import com.config.compare.service.ExternalLinkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 外部链接Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalLinkServiceImpl extends ServiceImpl<ExternalLinkMapper, ExternalLink> implements ExternalLinkService {

    private final ExternalLinkMapper externalLinkMapper;

    @Override
    public IPage<ExternalLink> pageQuery(PageRequest pageRequest, String linkName, Integer openType, Integer status) {
        Page<ExternalLink> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        
        LambdaQueryWrapper<ExternalLink> queryWrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(linkName)) {
            queryWrapper.like(ExternalLink::getLinkName, linkName);
        }
        if (openType != null) {
            queryWrapper.eq(ExternalLink::getOpenType, openType);
        }
        if (status != null) {
            queryWrapper.eq(ExternalLink::getStatus, status);
        }
        
        queryWrapper.orderByAsc(ExternalLink::getSortOrder);
        queryWrapper.orderByDesc(ExternalLink::getCreateTime);
        
        return page(page, queryWrapper);
    }

    @Override
    public List<ExternalLink> getEnabledLinks() {
        return externalLinkMapper.selectEnabledLinks();
    }

    @Override
    public boolean checkLinkNameExists(String linkName, Long excludeId) {
        return externalLinkMapper.countByLinkName(linkName, excludeId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createLink(ExternalLink externalLink) {
        // 设置默认值
        if (externalLink.getSortOrder() == null) {
            externalLink.setSortOrder(0);
        }
        if (externalLink.getParentId() == null) {
            externalLink.setParentId(0L);
        }
        if (externalLink.getIcon() == null) {
            externalLink.setIcon("Link");
        }
        if (externalLink.getStatus() == null) {
            externalLink.setStatus(1);
        }
        
        log.info("创建外部链接: {}", externalLink.getLinkName());
        return save(externalLink);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateLink(ExternalLink externalLink) {
        log.info("更新外部链接: id={}, name={}", externalLink.getId(), externalLink.getLinkName());
        return updateById(externalLink);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLink(Long id) {
        log.info("删除外部链接: id={}", id);
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteLinks(List<Long> ids) {
        log.info("批量删除外部链接: ids={}", ids);
        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, Integer status) {
        ExternalLink externalLink = new ExternalLink();
        externalLink.setId(id);
        externalLink.setStatus(status);
        log.info("更新外部链接状态: id={}, status={}", id, status);
        return updateById(externalLink);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSortOrder(Long id, Integer sortOrder) {
        ExternalLink externalLink = new ExternalLink();
        externalLink.setId(id);
        externalLink.setSortOrder(sortOrder);
        log.info("更新外部链接排序: id={}, sortOrder={}", id, sortOrder);
        return updateById(externalLink);
    }
}
