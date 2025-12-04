package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.config.compare.entity.CollectResultEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * 采集结果Mapper
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Mapper
public interface CollectResultEntityMapper extends BaseMapper<CollectResultEntity> {

    /**
     * 查询每个任务的最新采集结果（分页）
     *
     * @param page        分页对象
     * @param taskName    任务名称（可选）
     * @param collectType 采集类型（可选）
     * @param systemId    系统ID（可选）
     * @return 分页结果
     */
    IPage<Map<String, Object>> selectLatestResultsWithTask(
            Page<Map<String, Object>> page,
            @Param("taskName") String taskName,
            @Param("collectType") String collectType,
            @Param("systemId") Long systemId
    );
}