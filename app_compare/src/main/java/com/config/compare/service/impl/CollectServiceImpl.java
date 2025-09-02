package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.config.compare.collect.handler.CollectHandler;
import com.config.compare.collect.manager.CollectHandlerManager;
import com.config.compare.collect.model.CollectContext;
import com.config.compare.collect.model.CollectResult;
import com.config.compare.entity.*;
import com.config.compare.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 采集服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectServiceImpl implements CollectService {

    private final CollectHandlerManager handlerManager;
    private final CollectExecutionService collectExecutionService;
    private final CollectResultEntityService collectResultEntityService;
    private final ServerInstanceService serverInstanceService;
    private final CollectTemplateService collectTemplateService;

    @Autowired
    @Lazy
    private CompareTaskService compareTaskService;

    // 执行线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    
    // 正在执行的任务映射
    private final Map<String, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public boolean executeTask(Long taskId) {
        log.info("CollectService.executeTask 方法已弃用，请使用 CollectTaskService.executeTask");
        return false;
    }

    @Override
    public String executeTaskImmediately(CollectTask task) {
        String executeId = generateExecuteId();
        
        try {
            // 获取目标服务器列表
            List<ServerInstance> servers = getTaskTargetServers(task);
            if (servers.isEmpty()) {
                log.error("任务无可用的目标服务器：{}", task.getId());
                return null;
            }
            
            // 创建执行记录
            createExecutionRecord(task.getId(), executeId, servers.size());
            
            // 异步执行采集任务
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                executeTaskAsync(task, executeId, servers);
            }, executorService);
            
            runningTasks.put(executeId, future);
            
            return executeId;
            
        } catch (Exception e) {
            log.error("启动采集任务执行失败", e);
            updateExecutionRecord(executeId, 3, 0, 0, "启动执行失败：" + e.getMessage());
            return null;
        }
    }

    @Override
    public CollectResult executeSingleCollect(CollectContext context) {
        String typeCode = context.getCollectType();
        CollectHandler handler = handlerManager.getHandler(typeCode);
        
        if (handler == null) {
            log.error("找不到采集处理器：{}", typeCode);
            return CollectResult.fail("找不到采集处理器：" + typeCode);
        }
        
        try {
            return handler.collect(context);
        } catch (Exception e) {
            log.error("执行采集失败", e);
            return CollectResult.fail("执行采集失败：" + e.getMessage());
        }
    }

    @Override
    public Map<Long, CollectResult> executeBatchCollect(List<CollectContext> contexts) {
        Map<Long, CollectResult> results = new ConcurrentHashMap<>();
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (CollectContext context : contexts) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                CollectResult result = executeSingleCollect(context);
                results.put(context.getServerInstance().getId(), result);
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        return results;
    }

    @Override
    public boolean testCollectConfig(CollectContext context) {
        String typeCode = context.getCollectType();
        CollectHandler handler = handlerManager.getHandler(typeCode);
        
        if (handler == null) {
            log.error("找不到采集处理器：{}", typeCode);
            return false;
        }
        
        try {
            return handler.testConnection(context);
        } catch (Exception e) {
            log.error("测试采集配置失败", e);
            return false;
        }
    }

    @Override
    public boolean stopExecution(String executeId) {
        CompletableFuture<Void> future = runningTasks.get(executeId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                runningTasks.remove(executeId);
                updateExecutionRecord(executeId, 3, null, null, "执行被手动停止");
                log.info("执行已停止：{}", executeId);
                return true;
            }
        }
        return false;
    }

    @Override
    public CollectExecution getExecutionStatus(String executeId) {
        return collectExecutionService.getByExecuteId(executeId);
    }

    @Override
    public List<CollectResultEntity> getExecutionResults(String executeId) {
        return collectResultEntityService.listByExecuteId(executeId);
    }

    @Override
    public boolean saveCollectResult(Long taskId, String executeId, Long serverInstanceId, CollectResult result) {
        try {
            CollectResultEntity entity = new CollectResultEntity();
            entity.setTaskId(taskId);
            entity.setExecuteId(executeId);
            entity.setServerInstanceId(serverInstanceId);
            entity.setCollectItemName(result.getContent() != null ? "采集项" : "失败项"); // 简化处理
            entity.setCollectType("AUTO"); // 简化处理
            entity.setCollectContent(result.getContent());
            entity.setFilePath(result.getFilePath());
            entity.setApiEndpoint(result.getApiEndpoint());
            entity.setNamespace(result.getNamespace());
            entity.setCollectStatus(result.isSuccess() ? 1 : 0);
            entity.setErrorMessage(result.getErrorMessage());
            entity.setExecuteTime(result.getExecuteTime());
            entity.setDurationMs(result.getDurationMs());
            entity.setRetryCount(result.getRetryCount());
            
            return collectResultEntityService.save(entity);
            
        } catch (Exception e) {
            log.error("保存采集结果失败", e);
            return false;
        }
    }

    @Override
    public boolean createExecutionRecord(Long taskId, String executeId, int totalServers) {
        try {
            CollectExecution execution = new CollectExecution();
            execution.setTaskId(taskId);
            execution.setExecuteId(executeId);
            execution.setExecuteStatus(4); // 运行中
            execution.setTotalServers(totalServers);
            execution.setSuccessServers(0);
            execution.setFailedServers(0);
            execution.setStartTime(LocalDateTime.now());
            
            return collectExecutionService.save(execution);
            
        } catch (Exception e) {
            log.error("创建执行记录失败", e);
            return false;
        }
    }

    @Override
    public boolean updateExecutionRecord(String executeId, Integer status, Integer successCount, 
                                        Integer failedCount, String errorMessage) {
        try {
            CollectExecution execution = collectExecutionService.getByExecuteId(executeId);
            if (execution == null) {
                return false;
            }
            
            if (status != null) {
                execution.setExecuteStatus(status);
            }
            if (successCount != null) {
                execution.setSuccessServers(successCount);
            }
            if (failedCount != null) {
                execution.setFailedServers(failedCount);
            }
            if (StringUtils.hasText(errorMessage)) {
                execution.setErrorMessage(errorMessage);
            }
            
            return collectExecutionService.updateById(execution);
            
        } catch (Exception e) {
            log.error("更新执行记录失败", e);
            return false;
        }
    }

    @Override
    public boolean finishExecutionRecord(String executeId) {
        try {
            CollectExecution execution = collectExecutionService.getByExecuteId(executeId);
            if (execution == null) {
                return false;
            }
            
            execution.setEndTime(LocalDateTime.now());
            
            // 计算执行耗时
            if (execution.getStartTime() != null) {
                long duration = java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis();
                execution.setDurationMs(duration);
            }
            
            // 根据成功失败数量确定最终状态
            int totalServers = execution.getTotalServers();
            int successServers = execution.getSuccessServers();
            
            if (successServers == totalServers) {
                execution.setExecuteStatus(1); // 全部成功
            } else if (successServers > 0) {
                execution.setExecuteStatus(2); // 部分成功
            } else {
                execution.setExecuteStatus(3); // 全部失败
            }
            
            // 清理运行中的任务
            runningTasks.remove(executeId);
            
            return collectExecutionService.updateById(execution);
            
        } catch (Exception e) {
            log.error("完成执行记录失败", e);
            return false;
        }
    }

    @Override
    public boolean cleanupExpiredRecords(int retentionDays) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            // 清理执行记录
            collectExecutionService.deleteExpiredRecords(cutoffTime);
            
            // 清理采集结果
            collectResultEntityService.deleteExpiredResults(cutoffTime);
            
            log.info("清理过期记录完成，保留天数：{}", retentionDays);
            return true;
            
        } catch (Exception e) {
            log.error("清理过期记录失败", e);
            return false;
        }
    }

    /**
     * 异步执行任务
     */
    private void executeTaskAsync(CollectTask task, String executeId, List<ServerInstance> servers) {
        int successCount = 0;
        int failedCount = 0;
        
        try {
            log.info("开始执行采集任务，ID：{}，执行ID：{}，服务器数：{}", 
                     task.getId(), executeId, servers.size());
            
            // 获取采集模板
            CollectTemplate template = collectTemplateService.getById(task.getTemplateId());
            if (template == null) {
                throw new RuntimeException("采集模板不存在: " + task.getTemplateId());
            }
            
            // 根据模板创建采集上下文
            for (ServerInstance server : servers) {
                try {
                    // 根据采集模板创建实际的采集上下文
                    CollectContext context = new CollectContext(server, template.getTemplateName(), template.getTemplateType());
                    context.setExecuteId(executeId);
                    context.setTaskId(task.getId());
                    context.setTimeoutSeconds(task.getTimeoutSeconds() != null ? task.getTimeoutSeconds() : 300);
                    context.setRetryCount(task.getRetryCount() != null ? task.getRetryCount() : 2);
                    
                    // 解析模板配置参数
                    Map<String, Object> configParams = parseTemplateContent(template.getTemplateContent());
                    context.setConfigParams(configParams);
                    
                    CollectResult result = executeSingleCollect(context);
                    
                    // 保存结果
                    saveCollectResult(task.getId(), executeId, server.getId(), result);
                    
                    if (result.isSuccess()) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("执行服务器采集失败：{}", server.getId(), e);
                    failedCount++;
                }
                
                // 更新进度
                updateExecutionRecord(executeId, null, successCount, failedCount, null);
            }
            
            log.info("采集任务执行完成，成功：{}，失败：{}", successCount, failedCount);
            
        } catch (Exception e) {
            log.error("执行采集任务异常", e);
            updateExecutionRecord(executeId, 3, successCount, failedCount, "执行异常：" + e.getMessage());
        } finally {
            // 完成执行记录
            finishExecutionRecord(executeId);

            // 触发关联的比对任务（仅触发与该采集任务关联、启用且为触发执行的比对任务）
            triggerRelatedCompareTasks(task.getId(), executeId, successCount, servers.size());
        }
    }

    /**
     * 获取任务的目标服务器列表
     */
    private List<ServerInstance> getTaskTargetServers(CollectTask task) {
        List<ServerInstance> servers = new ArrayList<>();
        
        try {
            if (StringUtils.hasText(task.getServerInstanceIds())) {
                // 使用指定的服务器实例
                String[] instanceIds = task.getServerInstanceIds().split(",");
                for (String idStr : instanceIds) {
                    try {
                        Long instanceId = Long.parseLong(idStr.trim());
                        ServerInstance server = serverInstanceService.getById(instanceId);
                        if (server != null && server.getStatus() == 1) {
                            servers.add(server);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("无效的服务器实例ID：{}", idStr);
                    }
                }
            } else if (StringUtils.hasText(task.getServerTypeIds())) {
                // 使用服务器类型查询
                String[] typeIds = task.getServerTypeIds().split(",");
                for (String idStr : typeIds) {
                    try {
                        Long typeId = Long.parseLong(idStr.trim());
                        List<ServerInstance> typeServers = serverInstanceService.listBySystemAndType(task.getSystemId(), typeId);
                        servers.addAll(typeServers);
                    } catch (NumberFormatException e) {
                        log.warn("无效的服务器类型ID：{}", idStr);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("获取任务目标服务器失败", e);
        }
        
        return servers;
    }

    /**
     * 生成执行ID
     */
    private String generateExecuteId() {
        return "EXEC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 解析模板内容
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseTemplateContent(String templateContent) {
        try {
            if (templateContent != null && templateContent.trim().startsWith("{")) {
                return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(templateContent, Map.class);
            }
        } catch (Exception e) {
            log.warn("解析模板内容失败: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    /**
     * 触发与指定采集任务关联的比对任务（execute_type=3 且 status=1）
     * 说明：比对服务内部已改为只比对“最新一次采集”的结果，这里只负责触发一次执行。
     */
    private void triggerRelatedCompareTasks(Long collectTaskId, String executeId, int successCount, int totalCount) {
        try {
            if (successCount <= 0) {
                log.info("本次采集全部失败，不触发比对。collectTaskId={} executeId={}", collectTaskId, executeId);
                return;
            }

            LambdaQueryWrapper<CompareTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CompareTask::getCollectTaskId, collectTaskId)
                   .eq(CompareTask::getExecuteType, 3)
                   .eq(CompareTask::getStatus, 1);

            List<CompareTask> compareTasks = compareTaskService.list(wrapper);
            if (compareTasks == null || compareTasks.isEmpty()) {
                log.info("未找到需触发的比对任务。collectTaskId={} executeId={}", collectTaskId, executeId);
                return;
            }

            log.info("触发{}个比对任务。collectTaskId={} executeId={} success/total={}/{}",
                    compareTasks.size(), collectTaskId, executeId, successCount, totalCount);

            for (CompareTask ct : compareTasks) {
                try {
                    String compareExecId = compareTaskService.executeTask(ct.getId());
                    log.info("已触发比对任务：{}({}) -> compareExecuteId={}", ct.getTaskName(), ct.getId(), compareExecId);
                } catch (Exception ex) {
                    log.error("触发比对任务失败：{}({})", ct.getTaskName(), ct.getId(), ex);
                }
            }
        } catch (Exception e) {
            log.error("触发关联比对任务时异常。collectTaskId={} executeId={} ", collectTaskId, executeId, e);
        }
    }
}