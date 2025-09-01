package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.ServerInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 服务器实例Mapper接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Mapper
public interface ServerInstanceMapper extends BaseMapper<ServerInstance> {

    /**
     * 根据系统ID和服务器类型ID查询服务器实例列表
     * 
     * @param systemId 系统ID
     * @param serverTypeId 服务器类型ID
     * @return 服务器实例列表
     */
    List<ServerInstance> selectBySystemAndType(@Param("systemId") Long systemId, 
                                               @Param("serverTypeId") Long serverTypeId);

    /**
     * 根据系统ID查询服务器实例列表
     * 
     * @param systemId 系统ID
     * @return 服务器实例列表
     */
    List<ServerInstance> selectBySystemId(@Param("systemId") Long systemId);
}