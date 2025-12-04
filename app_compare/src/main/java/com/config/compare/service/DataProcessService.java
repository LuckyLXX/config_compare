package com.config.compare.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.config.compare.service.dto.AiProcessRequest;
import com.config.compare.service.dto.DataCleanRequest;
import com.config.compare.service.dto.DataProcessResponse;
import com.config.compare.service.dto.ExcelConvertRequest;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 数据处理服务接口
 *
 * @author system
 * @version 1.0.0
 * @since 2025-01-27
 */
public interface DataProcessService {

    /**
     * JSON转Excel
     *
     * @param request 转换请求
     * @return 处理响应
     */
    DataProcessResponse convertToExcel(ExcelConvertRequest request);

    /**
     * AI智能处理
     *
     * @param request AI处理请求
     * @return 处理响应
     */
    DataProcessResponse aiProcess(AiProcessRequest request);

    /**
     * 数据清洗
     *
     * @param request 清洗请求
     * @return 处理响应
     */
    DataProcessResponse cleanData(DataCleanRequest request);

    /**
     * 下载处理结果文件
     *
     * @param fileId   文件ID
     * @param response HTTP响应
     */
    void downloadFile(String fileId, HttpServletResponse response);

    /**
     * 获取临时文件存储路径
     *
     * @param fileId 文件ID
     * @return 文件路径
     */
    String getFilePath(String fileId);

    /**
     * 清理过期的临时文件
     *
     * @param expireMinutes 过期时间（分钟）
     * @return 清理的文件数量
     */
    int cleanExpiredFiles(int expireMinutes);

    /**
     * 获取数据处理任务列表（每个任务关联最新的采集结果）
     *
     * @param current      当前页
     * @param size         每页大小
     * @param taskName     任务名称（可选）
     * @param collectType  采集类型（可选）
     * @param systemId     系统ID（可选）
     * @return 分页结果
     */
    IPage<Map<String, Object>> getDataProcessTasks(Integer current, Integer size, 
            String taskName, String collectType, Long systemId);

    /**
     * 根据执行ID获取采集结果详情
     *
     * @param executeId 执行ID
     * @return 采集结果内容
     */
    Map<String, Object> getCollectResultByExecuteId(String executeId);

    /**
     * 测试AI服务连接
     *
     * @param url     API地址
     * @param apiKey  API密钥
     * @param modelId 模型标识
     * @param timeout 超时时间（秒）
     * @return 测试结果
     */
    Map<String, Object> testAiConnection(String url, String apiKey, String modelId, Integer timeout);
}
