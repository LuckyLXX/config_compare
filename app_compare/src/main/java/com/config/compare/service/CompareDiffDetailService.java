package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.config.compare.entity.CompareDiffDetail;

import java.util.List;

/**
 * 比对差异详情Service接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
public interface CompareDiffDetailService extends IService<CompareDiffDetail> {

    /**
     * 根据比对结果ID分页查询差异详情
     * 
     * @param resultId 比对结果ID
     * @param page 分页参数
     * @return 差异详情列表
     */
    IPage<CompareDiffDetail> getDiffDetailsByResultId(Long resultId, Page<CompareDiffDetail> page);

    /**
     * 根据比对结果ID查询所有差异详情
     * 
     * @param resultId 比对结果ID
     * @return 差异详情列表
     */
    List<CompareDiffDetail> getAllDiffDetailsByResultId(Long resultId);

    /**
     * 批量保存差异详情
     * 
     * @param resultId 比对结果ID
     * @param diffDetails 差异详情列表
     */
    void batchSaveDiffDetails(Long resultId, List<CompareDiffDetail> diffDetails);
}
