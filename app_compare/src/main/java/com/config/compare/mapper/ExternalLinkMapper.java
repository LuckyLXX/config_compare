package com.config.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.config.compare.entity.ExternalLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 外部链接Mapper接口
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-11-28
 */
@Mapper
public interface ExternalLinkMapper extends BaseMapper<ExternalLink> {

    /**
     * 查询所有启用的外部链接（按排序）
     * 
     * @return 外部链接列表
     */
    @Select("SELECT * FROM sys_external_link WHERE status = 1 ORDER BY sort_order ASC")
    List<ExternalLink> selectEnabledLinks();

    /**
     * 检查链接名称是否存在
     * 
     * @param linkName 链接名称
     * @param excludeId 排除的ID
     * @return 数量
     */
    @Select("<script>" +
            "SELECT COUNT(1) FROM sys_external_link WHERE link_name = #{linkName}" +
            "<if test='excludeId != null'> AND id != #{excludeId}</if>" +
            "</script>")
    int countByLinkName(@Param("linkName") String linkName, @Param("excludeId") Long excludeId);
}
