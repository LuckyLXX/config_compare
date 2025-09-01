package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CompareTask;
import com.config.compare.entity.CompareExecution;
import com.config.compare.entity.CompareResult;
import com.config.compare.entity.CompareRule;
import com.config.compare.mapper.CompareTaskMapper;
import com.config.compare.service.CompareTaskService;
import com.config.compare.service.SshTextCompareService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompareTaskServiceImpl extends ServiceImpl<CompareTaskMapper, CompareTask> implements CompareTaskService {

    private final SshTextCompareService sshTextCompareService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Override
    public IPage<CompareTask> pageQuery(int current, int size, String taskName, Long systemId, Integer executeType, Integer status) {
        Page<CompareTask> page = new Page<>(current, size);
        LambdaQueryWrapper<CompareTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(taskName != null && !taskName.isBlank(), CompareTask::getTaskName, taskName)
               .eq(systemId != null, CompareTask::getSystemId, systemId)
               .eq(executeType != null, CompareTask::getExecuteType, executeType)
               .eq(status != null, CompareTask::getStatus, status)
               .orderByDesc(CompareTask::getUpdateTime);
        return this.page(page, wrapper);
    }

    @Override
    @Transactional
    public boolean createTask(CompareTask task) {
        try {
            // 设置默认值
            if (task.getStatus() == null) {
                task.setStatus(1); // 默认启用
            }
            if (task.getAutoExecute() == null) {
                task.setAutoExecute(0); // 默认不自动执行
            }
            
            // 保存任务
            boolean success = this.save(task);
            if (success) {
                log.info("比对任务创建成功: taskId={}, taskName={}", task.getId(), task.getTaskName());
            }
            return success;
        } catch (Exception e) {
            log.error("创建比对任务失败: {}", task.getTaskName(), e);
            throw new RuntimeException("创建比对任务失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean updateTask(CompareTask task) {
        try {
            // 检查任务是否存在
            CompareTask existingTask = this.getById(task.getId());
            if (existingTask == null) {
                throw new RuntimeException("比对任务不存在: " + task.getId());
            }
            
            // 更新任务
            boolean success = this.updateById(task);
            if (success) {
                log.info("比对任务更新成功: taskId={}, taskName={}", task.getId(), task.getTaskName());
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
            // 检查任务是否存在
            CompareTask task = this.getById(id);
            if (task == null) {
                throw new RuntimeException("比对任务不存在: " + id);
            }
            
            // 检查任务状态，如果正在执行则不允许删除
            if (task.getStatus() == 1) {
                // TODO: 检查是否有正在执行的比对任务
                // 暂时允许删除
            }
            
            // 删除任务
            boolean success = this.removeById(id);
            if (success) {
                log.info("比对任务删除成功: taskId={}, taskName={}", id, task.getTaskName());
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
            
            // TODO: 实现具体的比对逻辑
            // 1. 获取基线配置
            // 2. 获取目标服务器列表
            // 3. 执行比对
            // 4. 保存比对结果
            
            log.info("比对任务执行完成: taskId={}, executeId={}", task.getId(), executeId);
            
        } catch (Exception e) {
            log.error("异步执行比对任务异常: taskId={}, executeId={}", task.getId(), executeId, e);
        }
    }

    /**
     * 执行SSH文本比对任务
     */
    @Transactional
    public CompareExecution executeSshTextCompare(Long taskId, CompareRule compareRule, 
                                                String baselineContent, List<String> collectedContents) {
        log.info("开始执行SSH文本比对任务: taskId={}", taskId);
        
        // 创建比对执行记录
        CompareExecution execution = new CompareExecution();
        execution.setTaskId(taskId);
        execution.setStatus(1); // 执行中
        execution.setStartTime(LocalDateTime.now());
        execution.setTotalServers(collectedContents.size());
        execution.setSuccessCount(0);
        execution.setFailedCount(0);
        execution.setDiffCount(0);
        execution.setCreateTime(LocalDateTime.now());
        execution.setUpdateTime(LocalDateTime.now());
        
        try {
            // 解析比对规则
            String compareMode = compareRule.getSshCompareMode();
            String ignoreLines = compareRule.getIgnoreLines();
            Double diffThreshold = compareRule.getDiffThreshold();
            String[] ignoreDifferences = parseIgnoreDifferences(compareRule.getIgnoreDifferences());
            
            // 并发执行比对
            List<CompletableFuture<CompareResult>> futures = new ArrayList<>();
            
            for (int i = 0; i < collectedContents.size(); i++) {
                final int serverIndex = i;
                final String collectedContent = collectedContents.get(i);
                
                CompletableFuture<CompareResult> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return sshTextCompareService.compareSshText(
                            baselineContent, 
                            collectedContent, 
                            compareMode, 
                            ignoreLines, 
                            diffThreshold, 
                            ignoreDifferences
                        );
                    } catch (Exception e) {
                        log.error("服务器{}比对失败", serverIndex, e);
                        CompareResult errorResult = new CompareResult();
                        errorResult.setStatus(2); // 失败
                        errorResult.setErrorMessage(e.getMessage());
                        errorResult.setCreateTime(LocalDateTime.now());
                        errorResult.setUpdateTime(LocalDateTime.now());
                        return errorResult;
                    }
                }, executorService);
                
                futures.add(future);
            }
            
            // 等待所有比对完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 收集结果
            List<CompareResult> results = new ArrayList<>();
            for (CompletableFuture<CompareResult> future : futures) {
                CompareResult result = future.get();
                results.add(result);
                
                // 统计结果
                if (result.getStatus() == 1) {
                    execution.setSuccessCount(execution.getSuccessCount() + 1);
                } else if (result.getStatus() == 2) {
                    execution.setFailedCount(execution.getFailedCount() + 1);
                } else if (result.getStatus() == 3) {
                    execution.setDiffCount(execution.getDiffCount() + 1);
                }
            }
            
            // 更新执行状态
            execution.setStatus(2); // 执行成功
            execution.setEndTime(LocalDateTime.now());
            execution.setDuration(System.currentTimeMillis() - execution.getStartTime().toInstant(java.time.ZoneOffset.UTC).toEpochMilli());
            execution.setUpdateTime(LocalDateTime.now());
            
            log.info("SSH文本比对任务执行完成: taskId={}, 成功={}, 失败={}, 有差异={}", 
                    taskId, execution.getSuccessCount(), execution.getFailedCount(), execution.getDiffCount());
            
        } catch (Exception e) {
            log.error("SSH文本比对任务执行失败: taskId={}", taskId, e);
            execution.setStatus(3); // 执行失败
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
            execution.setUpdateTime(LocalDateTime.now());
        }
        
        return execution;
    }

    /**
     * 解析忽略差异选项
     */
    private String[] parseIgnoreDifferences(String ignoreDifferencesJson) {
        try {
            if (ignoreDifferencesJson != null && !ignoreDifferencesJson.isEmpty()) {
                return objectMapper.readValue(ignoreDifferencesJson, String[].class);
            }
        } catch (JsonProcessingException e) {
            log.warn("解析忽略差异选项失败", e);
        }
        return new String[0];
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
    }
}


