package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.BaselineVersionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 基线版本切换日志Mapper
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-04
 */
@Mapper
public interface BaselineVersionLogMapper extends BaseMapper<BaselineVersionLog> {
}

