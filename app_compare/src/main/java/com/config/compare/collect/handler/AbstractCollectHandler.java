package com.config.compare.collect.handler;

import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 采集处理器抽象基类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
public abstract class AbstractCollectHandler implements CollectHandler {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CollectResult collect(CollectContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("开始执行采集，类型：{}，项目：{}，服务器：{}", 
                     getTypeCode(), context.getCollectItemName(), context.getServerInstance().getInstanceName());
            
            // 验证配置参数
            if (!validateContext(context)) {
                return CollectResult.fail("采集上下文验证失败");
            }
            
            // 执行具体的采集逻辑
            CollectResult result = doCollect(context);
            
            // 设置执行耗时
            result.setDuration(startTime);
            
            if (result.isSuccess()) {
                log.info("采集完成，类型：{}，项目：{}，耗时：{}ms", 
                         getTypeCode(), context.getCollectItemName(), result.getDurationMs());
            } else {
                log.error("采集失败，类型：{}，项目：{}，错误：{}", 
                          getTypeCode(), context.getCollectItemName(), result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("采集过程发生异常", e);
            CollectResult result = CollectResult.fail("采集过程发生异常：" + e.getMessage());
            result.setDuration(startTime);
            return result;
        }
    }

    @Override
    public boolean validateConfig(String config) {
        if (!StringUtils.hasText(config)) {
            return true; // 空配置认为有效
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(config);
            return validateConfigJson(jsonNode);
        } catch (Exception e) {
            log.error("配置参数JSON格式错误", e);
            return false;
        }
    }

    /**
     * 执行具体的采集逻辑（由子类实现）
     * 
     * @param context 采集上下文
     * @return 采集结果
     */
    protected abstract CollectResult doCollect(CollectContext context);

    /**
     * 验证采集上下文
     * 
     * @param context 采集上下文
     * @return 是否有效
     */
    protected boolean validateContext(CollectContext context) {
        if (context == null) {
            log.error("采集上下文为空");
            return false;
        }
        
        if (context.getServerInstance() == null) {
            log.error("服务器实例为空");
            return false;
        }
        
        if (!StringUtils.hasText(context.getCollectItemName())) {
            log.error("采集项名称为空");
            return false;
        }
        
        return validateSpecificContext(context);
    }

    /**
     * 验证特定类型的上下文（由子类实现）
     * 
     * @param context 采集上下文
     * @return 是否有效
     */
    protected boolean validateSpecificContext(CollectContext context) {
        return true;
    }

    /**
     * 验证配置JSON（由子类实现）
     * 
     * @param jsonNode 配置JSON节点
     * @return 是否有效
     */
    protected boolean validateConfigJson(JsonNode jsonNode) {
        return true;
    }

    /**
     * 执行重试逻辑
     * 
     * @param context 采集上下文
     * @param operation 采集操作
     * @return 采集结果
     */
    protected CollectResult executeWithRetry(CollectContext context, CollectOperation operation) {
        int maxRetries = Math.max(0, context.getRetryCount());
        CollectResult lastResult = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    log.info("执行第{}次重试，采集项：{}", attempt, context.getCollectItemName());
                    Thread.sleep(1000 * attempt); // 递增延迟
                }
                
                CollectResult result = operation.execute();
                if (result.isSuccess()) {
                    result.setRetryCount(attempt);
                    return result;
                }
                lastResult = result;
                
            } catch (Exception e) {
                log.error("采集执行异常，尝试次数：{}", attempt + 1, e);
                lastResult = CollectResult.fail("执行异常：" + e.getMessage());
                lastResult.setRetryCount(attempt);
            }
        }
        
        return lastResult != null ? lastResult : CollectResult.fail("未知错误");
    }

    /**
     * 采集操作函数式接口
     */
    @FunctionalInterface
    protected interface CollectOperation {
        CollectResult execute() throws Exception;
    }
}