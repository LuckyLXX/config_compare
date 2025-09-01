package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.compare.entity.CollectExecution;
import com.config.compare.entity.CollectResultEntity;
import com.config.compare.entity.CollectTask;
import com.config.compare.entity.CollectTemplate;
import com.config.compare.entity.SystemInfo;
import com.config.compare.mapper.CollectTaskMapper;
import com.config.compare.service.CollectExecutionService;
import com.config.compare.service.CollectService;
import com.config.compare.service.CollectTaskService;
import com.config.compare.service.CollectTemplateService;
import com.config.compare.service.SystemInfoService;
import com.config.compare.service.ServerInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采集任务Service实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectTaskServiceImpl extends ServiceImpl<CollectTaskMapper, CollectTask> implements CollectTaskService {

    private final CollectTemplateService collectTemplateService;
    private final SystemInfoService systemInfoService;
    private final CollectService collectService;
    private final CollectExecutionService collectExecutionService;
    private final ServerInstanceService serverInstanceService;

    @Override
    public IPage<Map<String, Object>> getTaskListWithDetails(int current, int size, String taskName, Long systemId, Integer executeType) {
        log.info("查询采集任务列表: current={}, size={}, taskName={}, systemId={}, executeType={}", 
                current, size, taskName, systemId, executeType);

        // 构建查询条件
        LambdaQueryWrapper<CollectTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(taskName), CollectTask::getTaskName, taskName)
               .eq(systemId != null, CollectTask::getSystemId, systemId)
               .eq(executeType != null, CollectTask::getExecuteType, executeType)
               .orderByDesc(CollectTask::getUpdateTime);

        // 分页查询
        Page<CollectTask> page = new Page<>(current, size);
        IPage<CollectTask> taskPage = this.page(page, wrapper);

        // 转换为包含详细信息的Map
        Page<Map<String, Object>> resultPage = new Page<>(current, size);
        resultPage.setTotal(taskPage.getTotal());
        resultPage.setCurrent(taskPage.getCurrent());
        resultPage.setSize(taskPage.getSize());

        List<Map<String, Object>> records = taskPage.getRecords().stream().map(task -> {
            Map<String, Object> record = new HashMap<>();
            record.put("id", task.getId());
            record.put("taskName", task.getTaskName());
            record.put("systemId", task.getSystemId());
            record.put("templateId", task.getTemplateId());
            record.put("executeType", task.getExecuteType());
            record.put("status", task.getStatus());
            record.put("cronExpression", task.getCronExpression());
            record.put("maxConcurrency", task.getMaxConcurrency());
            record.put("timeoutSeconds", task.getTimeoutSeconds());
            record.put("retryCount", task.getRetryCount());
            record.put("lastExecuteTime", task.getLastExecuteTime());
            record.put("nextExecuteTime", task.getNextExecuteTime());
            record.put("description", task.getDescription());
            record.put("createTime", task.getCreateTime());
            record.put("updateTime", task.getUpdateTime());
            
            // 添加执行状态相关信息
            record.put("executeStatus", getLatestExecuteStatus(task.getId()));
            record.put("lastExecuteId", getLatestExecuteId(task.getId()));
            record.put("totalServers", getExecuteServerCount(task.getId(), "total"));
            record.put("successServers", getExecuteServerCount(task.getId(), "success"));
            record.put("failedServers", getExecuteServerCount(task.getId(), "failed"));

            // 获取系统信息
            try {
                SystemInfo systemInfo = systemInfoService.getById(task.getSystemId());
                record.put("systemName", systemInfo != null ? systemInfo.getSystemName() : "未知系统");
            } catch (Exception e) {
                log.warn("获取系统信息失败: systemId={}", task.getSystemId(), e);
                record.put("systemName", "未知系统");
            }

            // 获取模板信息
            try {
                CollectTemplate template = collectTemplateService.getById(task.getTemplateId());
                record.put("templateName", template != null ? template.getTemplateName() : "未知模板");
            } catch (Exception e) {
                log.warn("获取模板信息失败: templateId={}", task.getTemplateId(), e);
                record.put("templateName", "未知模板");
            }

            return record;
        }).collect(Collectors.toList());

        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public Map<String, Object> getTaskWithDetails(Long id) {
        log.info("获取采集任务详情: id={}", id);

        CollectTask task = this.getById(id);
        if (task == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", task.getId());
        result.put("taskName", task.getTaskName());
        result.put("systemId", task.getSystemId());
        result.put("serverTypeIds", task.getServerTypeIds());
        result.put("serverInstanceIds", task.getServerInstanceIds());
        result.put("templateId", task.getTemplateId());
        result.put("cronExpression", task.getCronExpression());
        result.put("executeType", task.getExecuteType());
        result.put("maxConcurrency", task.getMaxConcurrency());
        result.put("timeoutSeconds", task.getTimeoutSeconds());
        result.put("retryCount", task.getRetryCount());
        result.put("status", task.getStatus());
        result.put("description", task.getDescription());
        result.put("createTime", task.getCreateTime());
        result.put("updateTime", task.getUpdateTime());

        // 获取关联信息
        try {
            SystemInfo systemInfo = systemInfoService.getById(task.getSystemId());
            result.put("systemName", systemInfo != null ? systemInfo.getSystemName() : null);

            CollectTemplate template = collectTemplateService.getById(task.getTemplateId());
            result.put("templateName", template != null ? template.getTemplateName() : null);
        } catch (Exception e) {
            log.warn("获取关联信息失败", e);
        }

        return result;
    }

    @Override
    public boolean createTask(CollectTask task) {
        log.info("创建采集任务: {}", task.getTaskName());
        
        // 设置默认值
        if (task.getStatus() == null) {
            task.setStatus(1); // 默认启用
        }
        if (task.getMaxConcurrency() == null) {
            task.setMaxConcurrency(5);
        }
        if (task.getTimeoutSeconds() == null) {
            task.setTimeoutSeconds(300);
        }
        if (task.getRetryCount() == null) {
            task.setRetryCount(2);
        }

        return this.save(task);
    }

    @Override
    public boolean updateTask(CollectTask task) {
        log.info("更新采集任务: id={}, name={}", task.getId(), task.getTaskName());
        return this.updateById(task);
    }

    @Override
    public boolean toggleTaskStatus(Long id, Integer status) {
        log.info("切换任务状态: id={}, status={}", id, status);
        
        CollectTask task = new CollectTask();
        task.setId(id);
        task.setStatus(status);
        
        return this.updateById(task);
    }

    @Override
    public String executeTask(Long id) {
        log.info("执行采集任务: id={}", id);
        
        CollectTask task = this.getById(id);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        if (task.getStatus() != 1) {
            throw new RuntimeException("任务未启用，无法执行");
        }
        
        try {
            // 更新最后执行时间
            task.setLastExecuteTime(LocalDateTime.now());
            this.updateById(task);
            
            // 使用CollectService执行真实的采集任务
            String executionId = collectService.executeTaskImmediately(task);
            
            if (executionId == null) {
                throw new RuntimeException("启动采集任务失败");
            }
            
            log.info("任务执行启动成功: id={}, executionId={}", id, executionId);
            return executionId;
            
        } catch (Exception e) {
            log.error("任务执行失败: id={}", id, e);
            throw new RuntimeException("任务执行失败: " + e.getMessage(), e);
        }
    }
    


    @Override
    public boolean batchExecute(List<Long> taskIds) {
        log.info("批量执行采集任务: taskIds={}", taskIds);
        
        for (Long taskId : taskIds) {
            try {
                executeTask(taskId);
            } catch (Exception e) {
                log.error("执行任务失败: taskId={}", taskId, e);
            }
        }
        
        return true;
    }

    @Override
    public boolean pauseTask(Long id) {
        log.info("暂停采集任务: id={}", id);
        // TODO: 实现任务暂停逻辑
        return true;
    }

    @Override
    public boolean resumeTask(Long id) {
        log.info("恢复采集任务: id={}", id);
        // TODO: 实现任务恢复逻辑
        return true;
    }

    @Override
    public IPage<Map<String, Object>> getTaskExecutions(Long taskId, int current, int size) {
        log.info("获取任务执行历史: taskId={}, current={}, size={}", taskId, current, size);
        
        try {
            // 从CollectExecution表中查询该任务的执行记录
            Page<CollectExecution> page = new Page<>(current, size);
            LambdaQueryWrapper<CollectExecution> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CollectExecution::getTaskId, taskId)
                   .orderByDesc(CollectExecution::getStartTime);
            
            IPage<CollectExecution> executionPage = collectExecutionService.page(page, wrapper);
            
            // 转换为包含详细信息的Map
            Page<Map<String, Object>> resultPage = new Page<>(current, size);
            resultPage.setTotal(executionPage.getTotal());
            resultPage.setCurrent(executionPage.getCurrent());
            resultPage.setSize(executionPage.getSize());
            
            List<Map<String, Object>> records = executionPage.getRecords().stream().map(execution -> {
                Map<String, Object> record = new HashMap<>();
                record.put("id", execution.getId());
                record.put("executeId", execution.getExecuteId());
                record.put("executeStatus", execution.getExecuteStatus());
                record.put("totalServers", execution.getTotalServers());
                record.put("successServers", execution.getSuccessServers());
                record.put("failedServers", execution.getFailedServers());
                record.put("startTime", execution.getStartTime());
                record.put("endTime", execution.getEndTime());
                record.put("durationMs", execution.getDurationMs());
                record.put("errorMessage", execution.getErrorMessage());
                
                // 添加状态描述
                String statusText = getExecuteStatusText(execution.getExecuteStatus());
                record.put("statusText", statusText);
                
                return record;
            }).collect(Collectors.toList());
            
            resultPage.setRecords(records);
            return resultPage;
        } catch (Exception e) {
            log.error("获取任务执行历史失败", e);
            Page<Map<String, Object>> page = new Page<>(current, size);
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        }
    }

    @Override
    public IPage<Map<String, Object>> getTaskResults(Long taskId, String executionId, int current, int size) {
        log.info("获取任务执行结果: taskId={}, executionId={}, current={}, size={}", 
                taskId, executionId, current, size);
        
        try {
            // 使用CollectService获取执行结果
            List<CollectResultEntity> results = collectService.getExecutionResults(executionId);

            // 预取服务器实例名称映射
            java.util.Set<Long> serverIds = results.stream()
                .map(CollectResultEntity::getServerInstanceId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
            Map<Long, String> serverIdToName = new java.util.HashMap<>();
            Map<Long, String> serverIdToIp = new java.util.HashMap<>();
            if (!serverIds.isEmpty()) {
                serverInstanceService.listByIds(serverIds).forEach(inst -> {
                    serverIdToName.put(inst.getId(), inst.getInstanceName());
                    serverIdToIp.put(inst.getId(), inst.getServerIp());
                });
            }
            
            // 转换为Map格式并分页
            List<Map<String, Object>> records = results.stream()
                .skip((long) (current - 1) * size)
                .limit(size)
                .map(result -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", result.getId());
                    record.put("serverInstanceId", result.getServerInstanceId());
                    String name = serverIdToName.get(result.getServerInstanceId());
                    String ip = serverIdToIp.get(result.getServerInstanceId());
                    record.put("serverName", name != null ? name : String.valueOf(result.getServerInstanceId()));
                    record.put("serverIp", ip);
                    record.put("collectItemName", result.getCollectItemName());
                    record.put("collectType", result.getCollectType());
                    record.put("collectStatus", result.getCollectStatus());
                    record.put("collectContent", result.getCollectContent());
                    record.put("filePath", result.getFilePath());
                    record.put("apiEndpoint", result.getApiEndpoint());
                    record.put("namespace", result.getNamespace());
                    record.put("errorMessage", result.getErrorMessage());
                    record.put("executeTime", result.getExecuteTime());
                    record.put("durationMs", result.getDurationMs());
                    record.put("retryCount", result.getRetryCount());
                    return record;
                })
                .collect(Collectors.toList());
                
            Page<Map<String, Object>> page = new Page<>(current, size);
            page.setTotal(results.size());
            page.setRecords(records);
            
            return page;
        } catch (Exception e) {
            log.error("获取任务执行结果失败", e);
            Page<Map<String, Object>> page = new Page<>(current, size);
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        }
    }
    
    /**
     * 获取任务最新执行状态
     */
    private Integer getLatestExecuteStatus(Long taskId) {
        try {
            CollectExecution execution = collectExecutionService.lambdaQuery()
                .eq(CollectExecution::getTaskId, taskId)
                .orderByDesc(CollectExecution::getStartTime)
                .last("LIMIT 1")
                .one();
            return execution != null ? execution.getExecuteStatus() : null;
        } catch (Exception e) {
            log.warn("获取任务最新执行状态失败: taskId={}", taskId, e);
            return null;
        }
    }
    
    /**
     * 获取任务最新执行ID
     */
    private String getLatestExecuteId(Long taskId) {
        try {
            // 查询最新的执行记录ID
            CollectExecution execution = collectExecutionService.lambdaQuery()
                .eq(CollectExecution::getTaskId, taskId)
                .orderByDesc(CollectExecution::getStartTime)
                .last("LIMIT 1")
                .one();
            return execution != null ? execution.getExecuteId() : null;
        } catch (Exception e) {
            log.warn("获取任务最新执行ID失败: taskId={}", taskId, e);
            return null;
        }
    }
    
    /**
     * 获取执行服务器统计
     */
    private Integer getExecuteServerCount(Long taskId, String type) {
        try {
            CollectExecution execution = collectExecutionService.lambdaQuery()
                .eq(CollectExecution::getTaskId, taskId)
                .orderByDesc(CollectExecution::getStartTime)
                .last("LIMIT 1")
                .one();
            if (execution == null) {
                return 0;
            }
            switch (type) {
                case "total":
                    return execution.getTotalServers();
                case "success":
                    return execution.getSuccessServers();
                case "failed":
                    return execution.getFailedServers();
                default:
                    return 0;
            }
        } catch (Exception e) {
            log.warn("获取执行服务器统计失败: taskId={}, type={}", taskId, type, e);
            return 0;
        }
    }
    
    /**
     * 获取执行状态文本
     */
    private String getExecuteStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1: return "成功";
            case 2: return "部分成功";
            case 3: return "失败";
            case 4: return "运行中";
            default: return "未知";
        }
    }
}