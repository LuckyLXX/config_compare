package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CompareDiffDetail;
import com.config.compare.mapper.CompareDiffDetailMapper;
import com.config.compare.service.CompareDiffDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比对差异详情Service实现
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class CompareDiffDetailServiceImpl extends ServiceImpl<CompareDiffDetailMapper, CompareDiffDetail> 
    implements CompareDiffDetailService {

    @Override
    public IPage<CompareDiffDetail> getDiffDetailsByResultId(Long resultId, Page<CompareDiffDetail> page) {
        LambdaQueryWrapper<CompareDiffDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CompareDiffDetail::getResultId, resultId)
                   .orderByDesc(CompareDiffDetail::getCreateTime);
        
        return this.page(page, queryWrapper);
    }

    @Override
    public List<CompareDiffDetail> getAllDiffDetailsByResultId(Long resultId) {
        LambdaQueryWrapper<CompareDiffDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CompareDiffDetail::getResultId, resultId)
                   .orderByDesc(CompareDiffDetail::getCreateTime);
        
        return this.list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveDiffDetails(Long resultId, List<CompareDiffDetail> diffDetails) {
        if (diffDetails == null || diffDetails.isEmpty()) {
            return;
        }

        // 设置结果ID和创建时间
        LocalDateTime now = LocalDateTime.now();
        for (CompareDiffDetail detail : diffDetails) {
            detail.setResultId(resultId);
            detail.setCreateTime(now);
            detail.setUpdateTime(now);
        }

        // 批量保存
        this.saveBatch(diffDetails);
        log.info("批量保存差异详情成功: resultId={}, count={}", resultId, diffDetails.size());
    }
}
