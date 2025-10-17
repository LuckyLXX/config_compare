package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CompareTask;
import com.config.compare.entity.CompareExecution;
import com.config.compare.entity.CompareResult;
import com.config.compare.entity.ConfigBaseline;
import com.config.compare.entity.ServerInstance;
import com.config.compare.entity.CollectTask;
import com.config.compare.entity.CollectResultEntity;
import com.config.compare.entity.CollectExecution;
import com.config.compare.mapper.CompareTaskMapper;
import com.config.compare.service.CompareTaskService;
import com.config.compare.service.ConfigBaselineService;
import com.config.compare.service.CollectTaskService;
import com.config.compare.service.ServerInstanceService;
import com.config.compare.service.CompareExecutionService;
import com.config.compare.service.CompareResultService;
import com.config.compare.service.CollectResultEntityService;
import com.config.compare.service.CompareDiffDetailService;
import com.config.compare.service.TaskSchedulerService;
import com.config.compare.service.CollectExecutionService;
import com.config.compare.entity.CompareDiffDetail;
import com.config.compare.compare.manager.CompareAlgorithmManager;
import com.config.compare.compare.algorithm.CompareAlgorithm;
import com.config.compare.compare.model.CompareContext;
import com.config.compare.compare.model.CompareResultModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 比对任务服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class CompareTaskServiceImpl extends ServiceImpl<CompareTaskMapper, CompareTask> implements CompareTaskService {

    @Autowired
    private ObjectMapper objectMapper;
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // 新增的依赖
    @Autowired
    private CompareAlgorithmManager compareAlgorithmManager;
    
    @Autowired
    private ConfigBaselineService configBaselineService;
    
    @Autowired
    private CollectTaskService collectTaskService;
    
    @Autowired
    private ServerInstanceService serverInstanceService;
    
    @Autowired
    private CompareExecutionService compareExecutionService;
    
    @Autowired
    private CompareResultService compareResultService;
    
    @Autowired
    private CollectResultEntityService collectResultEntityService;
    
    @Autowired
    private CollectExecutionService collectExecutionService;
    
    @Autowired
    private CompareDiffDetailService compareDiffDetailService;
    
    @Autowired
    @Lazy
    private TaskSchedulerService taskSchedulerService;

    @Override
    public IPage<CompareTask> pageQuery(int current, int size, String taskName, Long systemId, Integer executeType, Integer status) {
        try {
            Page<CompareTask> page = new Page<>(current, size);
            LambdaQueryWrapper<CompareTask> wrapper = new LambdaQueryWrapper<>();
            
            if (StringUtils.hasText(taskName)) {
                wrapper.like(CompareTask::getTaskName, taskName);
            }
            if (systemId != null) {
                wrapper.eq(CompareTask::getSystemId, systemId);
            }
            if (executeType != null) {
                wrapper.eq(CompareTask::getExecuteType, executeType);
            }
            if (status != null) {
                wrapper.eq(CompareTask::getStatus, status);
            }
            
            wrapper.orderByDesc(CompareTask::getCreateTime);
            return this.page(page, wrapper);
        } catch (Exception e) {
            log.error("获取比对任务列表失败", e);
            throw new RuntimeException("获取比对任务列表失败: " + e.getMessage());
        }
    }



    @Override
    @Transactional
    public boolean createTask(CompareTask task) {
        try {
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            boolean success = this.save(task);
            if (success) {
                log.info("创建比对任务成功: taskId={}", task.getId());
                
                // 如果是定时任务，注册到调度器
                if (task.getExecuteType() == 2 && task.getCronExpression() != null) {
                    taskSchedulerService.scheduleCompareTask(task);
                }
            }
            return success;
        } catch (Exception e) {
            log.error("创建比对任务失败", e);
            throw new RuntimeException("创建比对任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean updateTask(CompareTask task) {
        try {
            task.setUpdateTime(LocalDateTime.now());
            boolean success = this.updateById(task);
            if (success) {
                log.info("更新比对任务成功: taskId={}", task.getId());
                
                // 如果是定时任务，更新调度器
                if (task.getExecuteType() == 2 && task.getCronExpression() != null) {
                    taskSchedulerService.updateCompareTaskSchedule(task);
                }
            }
            return success;
        } catch (Exception e) {
            log.error("更新比对任务失败: taskId={}", task.getId(), e);
            throw new RuntimeException("更新比对任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteTask(Long id) {
        try {
            boolean success = this.removeById(id);
            if (success) {
                log.info("删除比对任务成功: taskId={}", id);
            }
            return success;
        } catch (Exception e) {
            log.error("删除比对任务失败: taskId={}", id, e);
            throw new RuntimeException("删除比对任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean toggleTaskStatus(Long id, Integer status) {
        try {
            // 检查任务是否存在
            CompareTask task = this.getById(id);
            if (task == null) {
                throw new RuntimeException("比对任务不存在: " + id);
            }
            
            // 更新状态
            task.setStatus(status);
            boolean success = this.updateById(task);
            if (success) {
                log.info("比对任务状态更新成功: taskId={}, status={}", id, status);
            }
            return success;
        } catch (Exception e) {
            log.error("更新比对任务状态失败: taskId={}, status={}", id, status, e);
            throw new RuntimeException("更新比对任务状态失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String executeTask(Long id) {
        try {
            // 检查任务是否存在
            CompareTask task = this.getById(id);
            if (task == null) {
                throw new RuntimeException("比对任务不存在: " + id);
            }
            
            // 检查任务状态
            if (task.getStatus() != 1) {
                throw new RuntimeException("比对任务未启用，无法执行: " + id);
            }
            
            // 生成执行ID
            String executeId = "COMPARE_" + System.currentTimeMillis();
            
            // TODO: 实现具体的比对任务执行逻辑
            log.info("开始执行比对任务: taskId={}, executeId={}", id, executeId);
            
            // 异步执行比对任务
            CompletableFuture.runAsync(() -> {
                try {
                    executeCompareTaskAsync(task, executeId);
                } catch (Exception e) {
                    log.error("异步执行比对任务失败: taskId={}, executeId={}", id, executeId, e);
                }
            }, executorService);
            
            return executeId;
        } catch (Exception e) {
            log.error("执行比对任务失败: taskId={}", id, e);
            throw new RuntimeException("执行比对任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean batchExecute(Long[] taskIds) {
        try {
            if (taskIds == null || taskIds.length == 0) {
                throw new RuntimeException("任务ID列表不能为空");
            }
            
            // 批量执行任务
            for (Long taskId : taskIds) {
                try {
                    executeTask(taskId);
                } catch (Exception e) {
                    log.error("批量执行任务失败: taskId={}", taskId, e);
                    // 继续执行其他任务
                }
            }
            
            log.info("批量执行比对任务完成: taskIds={}", StringUtils.arrayToCommaDelimitedString(taskIds));
            return true;
        } catch (Exception e) {
            log.error("批量执行比对任务失败: taskIds={}", StringUtils.arrayToCommaDelimitedString(taskIds), e);
            throw new RuntimeException("批量执行比对任务失败: " + e.getMessage());
        }
    }

    /**
     * 异步执行比对任务
     */
    private void executeCompareTaskAsync(CompareTask task, String executeId) {
        try {
            log.info("开始异步执行比对任务: taskId={}, executeId={}", task.getId(), executeId);
            
            // 1. 获取基线配置
            ConfigBaseline baseline = getBaselineForTask(task);
            if (baseline == null) {
                log.error("无法获取基线配置: taskId={}", task.getId());
                updateExecutionStatus(executeId, 3, "无法获取基线配置"); // 3表示失败
                return;
            }
            
            // 2. 获取目标服务器列表（从关联的采集任务）
            List<ServerInstance> targetServers = getTargetServersForTask(task);
            if (targetServers.isEmpty()) {
                log.error("无法获取目标服务器: taskId={}", task.getId());
                updateExecutionStatus(executeId, 3, "无法获取目标服务器");
                return;
            }
            
            // 3. 创建比对执行记录
            CompareExecution execution = createCompareExecution(task, executeId, baseline, targetServers.size());
            
            // 4. 执行比对
            List<CompareResult> compareResults = executeCompareForAllServers(task, executeId, baseline, targetServers);
            
            // 5. 更新执行记录
            updateExecutionWithResults(execution, compareResults);
            
            log.info("比对任务执行完成: taskId={}, executeId={}, 一致: {}, 不一致: {}, 失败: {}", 
                     task.getId(), executeId, execution.getConsistentServers(), 
                     execution.getInconsistentServers(), execution.getFailedServers());
            
        } catch (Exception e) {
            log.error("异步执行比对任务异常: taskId={}, executeId={}", task.getId(), executeId, e);
            updateExecutionStatus(executeId, 3, "执行异常：" + e.getMessage());
        }
    }

    // ==================== 新增的辅助方法 ====================

    /**
     * 获取任务的基线配置
     */
    private ConfigBaseline getBaselineForTask(CompareTask task) {
        try {
            if (task.getBaselineId() != null) {
                // 使用指定的基线
                return configBaselineService.getById(task.getBaselineId());
            } else {
                // 使用默认基线（根据系统ID和分类ID查找）
                // TODO: 需要传入serverTypeId，暂时使用null
                return configBaselineService.getDefaultBaseline(task.getSystemId(), null, task.getCategoryId());
            }
        } catch (Exception e) {
            log.error("获取基线配置失败: taskId={}", task.getId(), e);
            return null;
        }
    }

    /**
     * 获取任务的目标服务器列表
     */
    private List<ServerInstance> getTargetServersForTask(CompareTask task) {
        try {
            if (task.getCollectTaskId() != null) {
                // 从采集结果中获取实际采集到的服务器列表
                List<Long> collectedServerIds = getCollectedServerIds(task.getCollectTaskId());
                if (!collectedServerIds.isEmpty()) {
                    log.info("从采集结果获取服务器列表: taskId={}, collectedServerIds={}", task.getId(), collectedServerIds);
                    // 获取这些服务器的实例信息
                    List<ServerInstance> servers = new ArrayList<>();
                    for (Long serverId : collectedServerIds) {
                        ServerInstance server = serverInstanceService.getById(serverId);
                        if (server != null) {
                            servers.add(server);
                        }
                    }
                    return servers;
                } else {
                    log.warn("采集任务未产生采集结果: collectTaskId={}", task.getCollectTaskId());
                }
            }
            
            // 如果没有关联采集任务，返回空列表
            log.warn("比对任务未关联采集任务: taskId={}", task.getId());
            return new ArrayList<>();
            
        } catch (Exception e) {
            log.error("获取目标服务器失败: taskId={}", task.getId(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 创建比对执行记录
     */
    private CompareExecution createCompareExecution(CompareTask task, String executeId, 
                                                   ConfigBaseline baseline, int totalServers) {
        try {
            CompareExecution execution = new CompareExecution();
            execution.setTaskId(task.getId());
            execution.setExecuteId(executeId);
            execution.setBaselineId(baseline.getId());
            execution.setBaselineVersion(baseline.getBaselineVersion() != null ? baseline.getBaselineVersion() : "1.0");
            execution.setExecuteStatus(1); // 执行中
            execution.setTotalServers(totalServers);
            execution.setConsistentServers(0);
            execution.setInconsistentServers(0);
            execution.setFailedServers(0);
            execution.setOverallScore(java.math.BigDecimal.ZERO);
            execution.setStartTime(LocalDateTime.now());
            
            // 保存执行记录
            compareExecutionService.save(execution);
            log.info("创建比对执行记录: executeId={}, taskId={}", executeId, task.getId());
            
            return execution;
            
        } catch (Exception e) {
            log.error("创建比对执行记录失败: executeId={}", executeId, e);
            throw new RuntimeException("创建执行记录失败", e);
        }
    }

    /**
     * 为所有服务器执行比对
     */
    private List<CompareResult> executeCompareForAllServers(CompareTask task, String executeId, 
                                                           ConfigBaseline baseline, List<ServerInstance> servers) {
        List<CompareResult> results = new ArrayList<>();
        
        try {
            // 解析比对规则
            Map<String, Object> compareRules = parseCompareRules(task.getCompareRules());
            
            // 并发执行比对
            List<CompletableFuture<CompareResult>> futures = new ArrayList<>();
            
            for (ServerInstance server : servers) {
                CompletableFuture<CompareResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return executeCompareForServer(task, executeId, baseline, server, compareRules);
                    } catch (Exception e) {
                        log.error("服务器比对失败: serverId={}, executeId={}", server.getId(), executeId, e);
                        return createErrorCompareResult(task.getId(), executeId, baseline.getId(), server.getId(), e.getMessage());
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // 等待所有比对完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 收集结果
            for (CompletableFuture<CompareResult> future : futures) {
                CompareResult result = future.get();
                results.add(result);
            }
            
            log.info("所有服务器比对完成: executeId={}, 服务器数量={}", executeId, results.size());
            
        } catch (Exception e) {
            log.error("执行服务器比对失败: executeId={}", executeId, e);
        }
        
        return results;
    }

    /**
     * 为单个服务器执行比对
     */
    private CompareResult executeCompareForServer(CompareTask task, String executeId, 
                                                ConfigBaseline baseline, ServerInstance server, 
                                                Map<String, Object> compareRules) {
        try {
            log.info("开始为服务器执行比对: serverId={}, executeId={}", server.getId(), executeId);
            
            // 1. 获取采集结果
            String collectedContent = getCollectedContentForServer(task, server);
            if (collectedContent == null) {
                return createErrorCompareResult(task.getId(), executeId, baseline.getId(), server.getId(), "无法获取采集内容");
            }
            
            // 2. 创建比对上下文
            CompareContext context = new CompareContext(baseline, collectedContent, server);
            context.setCompareRules(compareRules);
            context.setExecuteId(executeId);
            context.setTaskId(task.getId());
            context.setContentType(determineContentType(baseline, collectedContent));
            
            // 3. 执行比对
            CompareResultModel compareResult = executeCompareWithAlgorithm(context);
            
            // 4. 保存比对结果
            return saveCompareResult(task.getId(), executeId, baseline.getId(), server.getId(), compareResult);
            
        } catch (Exception e) {
            log.error("服务器比对执行失败: serverId={}, executeId={}", server.getId(), executeId, e);
            return createErrorCompareResult(task.getId(), executeId, baseline.getId(), server.getId(), e.getMessage());
        }
    }

    /**
     * 执行比对（使用算法管理器）
     */
    private CompareResultModel executeCompareWithAlgorithm(CompareContext context) {
        try {
            // 使用算法管理器智能选择算法
            CompareAlgorithm algorithm = compareAlgorithmManager.getAlgorithmByContentType(context.getContentType());
            if (algorithm == null) {
                log.warn("未找到合适的比对算法，使用默认文本算法: contentType={}", context.getContentType());
                algorithm = compareAlgorithmManager.getAlgorithm("TEXT");
            }
            
            log.info("使用比对算法: algorithmType={}, algorithmName={}", 
                     algorithm.getAlgorithmType(), algorithm.getAlgorithmName());
            
            // 执行比对
            return algorithm.compare(context);
            
        } catch (Exception e) {
            log.error("执行比对算法失败", e);
            return CompareResultModel.fail("比对算法执行失败：" + e.getMessage());
        }
    }

    /**
     * 保存比对结果
     */
    private CompareResult saveCompareResult(Long taskId, String executeId, Long baselineId, 
                                          Long serverId, CompareResultModel compareResult) {
        try {
            CompareResult result = new CompareResult();
            result.setTaskId(taskId);
            result.setExecuteId(executeId);
            result.setBaselineId(baselineId);
            result.setServerInstanceId(serverId);
            // 获取比对任务以获取采集任务ID
            CompareTask compareTask = this.getById(taskId);
            Long collectTaskId = compareTask != null ? compareTask.getCollectTaskId() : null;
            result.setCollectResultId(getCollectResultId(collectTaskId, serverId));
            
            if (compareResult.isSuccess()) {
                result.setCompareStatus(compareResult.isConsistent() ? 1 : 0); // 1一致 0不一致
                result.setConsistencyScore(compareResult.getConsistencyScore());
                result.setDiffCount(compareResult.getDiffCount());
                result.setHighDiffCount(compareResult.getHighDiffCount());
                result.setMediumDiffCount(compareResult.getMediumDiffCount());
                result.setLowDiffCount(compareResult.getLowDiffCount());
                result.setDiffSummary(compareResult.getDiffSummary());
            } else {
                result.setCompareStatus(-1); // -1比对失败
                result.setErrorMessage(compareResult.getErrorMessage());
            }
            
            result.setExecuteTime(LocalDateTime.now());
            result.setDurationMs(compareResult.getDurationMs());
            result.setCreateTime(LocalDateTime.now());
            result.setUpdateTime(LocalDateTime.now());
            
            // 保存结果
            compareResultService.save(result);
            log.info("保存比对结果: resultId={}, serverId={}", result.getId(), serverId);
            
            // 保存差异详情
            if (compareResult.isSuccess() && compareResult.getDiffItems() != null && !compareResult.getDiffItems().isEmpty()) {
                List<CompareDiffDetail> diffDetails = convertDiffItemsToDetails(compareResult.getDiffItems());
                compareDiffDetailService.batchSaveDiffDetails(result.getId(), diffDetails);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("保存比对结果失败: serverId={}", serverId, e);
            throw new RuntimeException("保存比对结果失败", e);
        }
    }

    /**
     * 创建错误比对结果
     */
    private CompareResult createErrorCompareResult(Long taskId, String executeId, Long baselineId, 
                                                 Long serverId, String errorMessage) {
        CompareResult result = new CompareResult();
        result.setTaskId(taskId);
        result.setExecuteId(executeId);
        result.setBaselineId(baselineId);
        result.setServerInstanceId(serverId);
        result.setCollectResultId(0L);
        result.setCompareStatus(-1); // 比对失败
        result.setErrorMessage(errorMessage);
        result.setExecuteTime(LocalDateTime.now());
        result.setCreateTime(LocalDateTime.now());
        result.setUpdateTime(LocalDateTime.now());
        
        try {
            compareResultService.save(result);
        } catch (Exception e) {
            log.error("保存错误比对结果失败: serverId={}", serverId, e);
        }
        
        return result;
    }

    /**
     * 更新执行状态
     */
    private void updateExecutionStatus(String executeId, Integer status, String errorMessage) {
        try {
            CompareExecution execution = compareExecutionService.getByExecuteId(executeId);
            if (execution != null) {
                execution.setExecuteStatus(status);
                if (errorMessage != null) {
                    execution.setErrorMessage(errorMessage);
                }
                execution.setEndTime(LocalDateTime.now());
                execution.setUpdateTime(LocalDateTime.now());
                compareExecutionService.updateById(execution);
            }
        } catch (Exception e) {
            log.error("更新执行状态失败: executeId={}", executeId, e);
        }
    }

    /**
     * 更新执行记录结果
     */
    private void updateExecutionWithResults(CompareExecution execution, List<CompareResult> results) {
        try {
            int consistentCount = 0;
            int inconsistentCount = 0;
            int failedCount = 0;
            java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
            int validScoreCount = 0;
            
            for (CompareResult result : results) {
                if (result.getCompareStatus() == 1) {
                    consistentCount++;
                } else if (result.getCompareStatus() == 0) {
                    inconsistentCount++;
                } else if (result.getCompareStatus() == -1) {
                    failedCount++;
                }
                
                if (result.getConsistencyScore() != null) {
                    totalScore = totalScore.add(result.getConsistencyScore());
                    validScoreCount++;
                }
            }
            
            execution.setConsistentServers(consistentCount);
            execution.setInconsistentServers(inconsistentCount);
            execution.setFailedServers(failedCount);
            
            if (validScoreCount > 0) {
                execution.setOverallScore(totalScore.divide(java.math.BigDecimal.valueOf(validScoreCount), 2, java.math.BigDecimal.ROUND_HALF_UP));
            }
            
            execution.setExecuteStatus(2); // 执行完成
            execution.setEndTime(LocalDateTime.now());
            execution.setDurationMs(System.currentTimeMillis() - execution.getStartTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
            execution.setUpdateTime(LocalDateTime.now());
            
            compareExecutionService.updateById(execution);
            
            log.info("更新执行记录结果: executeId={}, 一致={}, 不一致={}, 失败={}, 平均分={}", 
                     execution.getExecuteId(), consistentCount, inconsistentCount, failedCount, execution.getOverallScore());
            
        } catch (Exception e) {
            log.error("更新执行记录结果失败: executeId={}", execution.getExecuteId(), e);
        }
    }

    /**
     * 解析比对规则
     */
    private Map<String, Object> parseCompareRules(String compareRulesJson) {
        try {
            if (compareRulesJson != null && !compareRulesJson.isEmpty()) {
                return objectMapper.readValue(compareRulesJson, Map.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("解析比对规则失败", e);
        }
        return new HashMap<>();
    }

    /**
     * 获取服务器的采集内容
     */
    private String getCollectedContentForServer(CompareTask task, ServerInstance server) {
        try {
            if (task.getCollectTaskId() != null) {
                // 查询采集结果表，获取该服务器的最新成功采集结果
                LambdaQueryWrapper<CollectResultEntity> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(CollectResultEntity::getTaskId, task.getCollectTaskId())
                           .eq(CollectResultEntity::getServerInstanceId, server.getId())
                           .eq(CollectResultEntity::getCollectStatus, 1) // 只获取成功的采集结果
                           .orderByDesc(CollectResultEntity::getExecuteTime)
                           .last("LIMIT 1");
                
                CollectResultEntity collectResult = collectResultEntityService.getOne(queryWrapper);
                if (collectResult != null && collectResult.getCollectContent() != null) {
                    log.info("获取到采集内容: serverId={}, contentLength={}", server.getId(), collectResult.getCollectContent().length());
                    return collectResult.getCollectContent();
                } else {
                    log.warn("未找到服务器的采集结果: serverId={}, collectTaskId={}", server.getId(), task.getCollectTaskId());
                }
            }
            
            return null;
        } catch (Exception e) {
            log.error("获取采集内容失败: serverId={}", server.getId(), e);
            return null;
        }
    }

    /**
     * 获取采集任务实际采集到的服务器ID列表（只获取最新一次采集的结果）
     */
    private List<Long> getCollectedServerIds(Long collectTaskId) {
        List<Long> serverIds = new ArrayList<>();
        try {
            // 首先获取最新一次采集的执行ID
            LambdaQueryWrapper<CollectExecution> executionQuery = new LambdaQueryWrapper<>();
            executionQuery.eq(CollectExecution::getTaskId, collectTaskId)
                          .eq(CollectExecution::getExecuteStatus, 1) // 全部成功
                          .or()
                          .eq(CollectExecution::getExecuteStatus, 2) // 部分成功
                          .orderByDesc(CollectExecution::getStartTime)
                          .last("LIMIT 1");
            
            CollectExecution latestExecution = collectExecutionService.getOne(executionQuery);
            if (latestExecution == null) {
                log.warn("未找到采集任务的执行记录: collectTaskId={}", collectTaskId);
                return serverIds;
            }
            
            log.info("找到最新采集执行记录: executeId={}, startTime={}", 
                     latestExecution.getExecuteId(), latestExecution.getStartTime());
            
            // 根据最新执行ID查询采集结果
            LambdaQueryWrapper<CollectResultEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CollectResultEntity::getExecuteId, latestExecution.getExecuteId())
                       .eq(CollectResultEntity::getCollectStatus, 1) // 只获取成功的采集结果
                       .select(CollectResultEntity::getServerInstanceId);
            
            List<CollectResultEntity> results = collectResultEntityService.list(queryWrapper);
            for (CollectResultEntity result : results) {
                serverIds.add(result.getServerInstanceId());
            }
            
            log.info("获取到最新采集结果中的服务器ID: collectTaskId={}, executeId={}, serverIds={}", 
                     collectTaskId, latestExecution.getExecuteId(), serverIds);
            
        } catch (Exception e) {
            log.error("获取采集结果中的服务器ID失败: collectTaskId={}", collectTaskId, e);
        }
        return serverIds;
    }

    /**
     * 解析服务器ID列表
     */
    private List<Long> parseServerIds(String serverIdsStr) {
        List<Long> serverIds = new ArrayList<>();
        try {
            if (serverIdsStr != null && !serverIdsStr.trim().isEmpty()) {
                String[] ids = serverIdsStr.split(",");
                for (String id : ids) {
                    try {
                        serverIds.add(Long.parseLong(id.trim()));
                    } catch (NumberFormatException e) {
                        log.warn("解析服务器ID失败: {}", id);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析服务器ID列表失败: {}", serverIdsStr, e);
        }
        return serverIds;
    }

    /**
     * 转换差异项为差异详情
     */
    private List<CompareDiffDetail> convertDiffItemsToDetails(List<com.config.compare.compare.model.DiffItem> diffItems) {
        List<CompareDiffDetail> details = new ArrayList<>();
        
        for (com.config.compare.compare.model.DiffItem diffItem : diffItems) {
            CompareDiffDetail detail = new CompareDiffDetail();
            detail.setDiffType(diffItem.getDiffType());
            detail.setDiffKey(diffItem.getDiffKey());
            detail.setDiffLevel(diffItem.getDiffLevel());
            detail.setDiffCategory(diffItem.getDiffCategory());
            detail.setDescription(diffItem.getDescription());
            detail.setBaselineValue(diffItem.getBaselineValue());
            detail.setCurrentValue(diffItem.getCurrentValue());
            detail.setSuggestAction(diffItem.getSuggestAction());
            detail.setDiffPath(diffItem.getDiffPath());
            details.add(detail);
        }
        
        return details;
    }

    /**
     * 获取采集结果ID
     */
    private Long getCollectResultId(Long taskId, Long serverId) {
        try {
            // 查询采集结果表，获取该服务器的最新成功采集结果ID
            LambdaQueryWrapper<CollectResultEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CollectResultEntity::getTaskId, taskId)
                       .eq(CollectResultEntity::getServerInstanceId, serverId)
                       .eq(CollectResultEntity::getCollectStatus, 1) // 只获取成功的采集结果
                       .orderByDesc(CollectResultEntity::getExecuteTime)
                       .select(CollectResultEntity::getId)
                       .last("LIMIT 1");
            
            CollectResultEntity collectResult = collectResultEntityService.getOne(queryWrapper);
            return collectResult != null ? collectResult.getId() : 0L;
            
        } catch (Exception e) {
            log.error("获取采集结果ID失败: taskId={}, serverId={}", taskId, serverId, e);
            return 0L;
        }
    }

    /**
     * 确定内容类型
     */
    private String determineContentType(ConfigBaseline baseline, String collectedContent) {
        // 根据基线配置或内容特征确定类型
        // TODO: ConfigBaseline实体类中没有configType字段，暂时返回TEXT
        
        // 增强的内容类型检测
        if (collectedContent != null) {
            String trimmed = collectedContent.trim();
            
            // JSON格式检测
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                // 进一步验证是否为有效的JSON
                try {
                    objectMapper.readTree(collectedContent);
                    return "JSON";
                } catch (Exception e) {
                    log.debug("内容不是有效的JSON，使用文本比对: {}", e.getMessage());
                }
            }
            
            // Apollo配置特殊检测 - 检查是否包含Apollo特有的结构
            if (trimmed.contains("application") &&
                (trimmed.contains("configServiceUrl") || trimmed.contains("appId") ||
                 trimmed.contains("cluster") || trimmed.contains("namespaces"))) {
                log.info("检测到Apollo配置特征，使用JSON比对算法");
                return "JSON";
            }
        }
        
        return "TEXT"; // 默认为文本类型
    }
}



