package com.config.compare.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.config.compare.entity.CollectTask;
import com.config.compare.entity.CompareTask;
import com.config.compare.service.CollectTaskService;
import com.config.compare.service.CompareTaskService;
import com.config.compare.service.TaskSchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时任务调度服务实现类
 * 
 * @author system
 * @version 1.0.0
 * @since 2025-01-25
 */
@Slf4j
@Service
public class TaskSchedulerServiceImpl implements TaskSchedulerService {

    @Autowired
    @Lazy
    private CollectTaskService collectTaskService;
    
    @Autowired
    @Lazy
    private CompareTaskService compareTaskService;

    // 存储定时任务的状态（用于快速查询）
    private final ConcurrentHashMap<String, Boolean> scheduledTasks = new ConcurrentHashMap<>();
    
    // 存储任务最后执行时间，防止重复执行
    private final ConcurrentHashMap<String, LocalDateTime> lastExecutionTimes = new ConcurrentHashMap<>();

    /**
     * 每分钟检查一次定时任务
     */
    @Scheduled(fixedDelay = 60000) // 60秒
    public void checkScheduledTasks() {
        try {
            log.debug("开始检查定时任务...");
            
            // 检查采集任务
            checkCollectTasks();
            
            // 检查比对任务
            checkCompareTasks();
            
            log.debug("定时任务检查完成");
        } catch (Exception e) {
            log.error("检查定时任务时发生错误", e);
        }
    }

    /**
     * 检查采集任务
     */
    private void checkCollectTasks() {
        List<CollectTask> scheduledTasks = getScheduledCollectTasks();
        for (CollectTask task : scheduledTasks) {
            try {
                String taskKey = "collect_" + task.getId();
                LocalDateTime lastExecution = lastExecutionTimes.get(taskKey);
                LocalDateTime now = LocalDateTime.now();
                
                // 防止重复执行：同一任务在5分钟内不重复执行
                if (lastExecution != null && lastExecution.plusMinutes(5).isAfter(now)) {
                    log.debug("任务 {} 在5分钟内已执行过，跳过: {}", task.getTaskName(), lastExecution);
                    continue;
                }
                
                if (isTimeToExecute(task.getCronExpression())) {
                    log.info("执行定时采集任务: {}", task.getTaskName());
                    collectTaskService.executeTask(task.getId());
                    lastExecutionTimes.put(taskKey, now);
                }
            } catch (Exception e) {
                log.error("执行定时采集任务失败: {}", task.getTaskName(), e);
            }
        }
    }

    /**
     * 检查比对任务
     */
    private void checkCompareTasks() {
        List<CompareTask> scheduledTasks = getScheduledCompareTasks();
        for (CompareTask task : scheduledTasks) {
            try {
                String taskKey = "compare_" + task.getId();
                LocalDateTime lastExecution = lastExecutionTimes.get(taskKey);
                LocalDateTime now = LocalDateTime.now();
                
                // 防止重复执行：同一任务在5分钟内不重复执行
                if (lastExecution != null && lastExecution.plusMinutes(5).isAfter(now)) {
                    log.debug("任务 {} 在5分钟内已执行过，跳过: {}", task.getTaskName(), lastExecution);
                    continue;
                }
                
                if (isTimeToExecute(task.getCronExpression())) {
                    log.info("执行定时比对任务: {}", task.getTaskName());
                    compareTaskService.executeTask(task.getId());
                    lastExecutionTimes.put(taskKey, now);
                }
            } catch (Exception e) {
                log.error("执行定时比对任务失败: {}", task.getTaskName(), e);
            }
        }
    }

    /**
     * 判断是否到了执行时间
     */
    private boolean isTimeToExecute(String cronExpression) {
        try {
            // 简单的Cron表达式解析（支持常见的格式）
            boolean exactMatch = parseCronExpression(cronExpression);
            
            if (exactMatch) {
                return true;
            }
            
            // 如果精确匹配失败，尝试宽松匹配（允许前后1分钟的时间窗口）
            return parseCronExpressionWithTolerance(cronExpression, 1);
        } catch (Exception e) {
            log.error("解析Cron表达式失败: {}", cronExpression, e);
            return false;
        }
    }

    /**
     * 带容差的Cron表达式解析（允许前后指定分钟的时间窗口）
     */
    private boolean parseCronExpressionWithTolerance(String cronExpression, int toleranceMinutes) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        
        log.debug("开始时间窗口匹配检查，容差: {}分钟", toleranceMinutes);
        
        // 检查前后toleranceMinutes分钟的时间窗口
        for (int offset = -toleranceMinutes; offset <= toleranceMinutes; offset++) {
            LocalDateTime checkTime = now.plusMinutes(offset);
            int currentSecond = checkTime.getSecond();
            int currentMinute = checkTime.getMinute();
            int currentHour = checkTime.getHour();
            int currentDay = checkTime.getDayOfMonth();
            int currentMonth = checkTime.getMonthValue();
            int currentDayOfWeek = checkTime.getDayOfWeek().getValue();

            log.debug("检查时间窗口偏移: {}分钟, 时间: {}:{}:{}, 日期: {}/{}, 星期: {}", 
                    offset, currentHour, currentMinute, currentSecond, currentDay, currentMonth, currentDayOfWeek);

            // 在时间窗口匹配中，我们忽略秒数的精确匹配，只要求分钟级别匹配
            // 检查分钟
            if (!matchesCronField(parts[1], currentMinute)) {
                log.debug("分钟不匹配: 期望={}, 实际={}", parts[1], currentMinute);
                continue;
            }

            // 检查小时
            if (!matchesCronField(parts[2], currentHour)) {
                log.debug("小时不匹配: 期望={}, 实际={}", parts[2], currentHour);
                continue;
            }

            // 检查日期
            if (!matchesCronField(parts[3], currentDay)) {
                log.debug("日期不匹配: 期望={}, 实际={}", parts[3], currentDay);
                continue;
            }

            // 检查月份
            if (!matchesCronField(parts[4], currentMonth)) {
                log.debug("月份不匹配: 期望={}, 实际={}", parts[4], currentMonth);
                continue;
            }

            // 检查星期（Cron中0=Sunday，Java中7=Sunday）
            int cronDayOfWeek = currentDayOfWeek == 7 ? 0 : currentDayOfWeek;
            if (!matchesCronField(parts[5], cronDayOfWeek)) {
                log.debug("星期不匹配: 期望={}, 实际={}", parts[5], cronDayOfWeek);
                continue;
            }

            log.info("Cron表达式在时间窗口内匹配成功: {} (偏移: {}分钟)", cronExpression, offset);
            return true;
        }

        log.debug("时间窗口内没有找到匹配的时间");
        return false;
    }

    /**
     * 简单的Cron表达式解析
     * 格式: 秒 分 时 日 月 周
     * 示例: "0 0 12 * * ?" (每天12点执行)
     */
    private boolean parseCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 6) {
            log.warn("Cron表达式格式错误，需要6个字段: {}", cronExpression);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        int currentSecond = now.getSecond();
        int currentMinute = now.getMinute();
        int currentHour = now.getHour();
        int currentDay = now.getDayOfMonth();
        int currentMonth = now.getMonthValue();
        int currentDayOfWeek = now.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday

        log.debug("当前时间: {}:{}:{}, 日期: {}/{}, 星期: {}", 
                currentHour, currentMinute, currentSecond, currentDay, currentMonth, currentDayOfWeek);
        log.debug("Cron表达式: {}", cronExpression);

        // 检查秒
        if (!matchesCronField(parts[0], currentSecond)) {
            log.debug("秒不匹配: 期望={}, 实际={}", parts[0], currentSecond);
            return false;
        }

        // 检查分钟
        if (!matchesCronField(parts[1], currentMinute)) {
            log.debug("分钟不匹配: 期望={}, 实际={}", parts[1], currentMinute);
            return false;
        }

        // 检查小时
        if (!matchesCronField(parts[2], currentHour)) {
            log.debug("小时不匹配: 期望={}, 实际={}", parts[2], currentHour);
            return false;
        }

        // 检查日期
        if (!matchesCronField(parts[3], currentDay)) {
            log.debug("日期不匹配: 期望={}, 实际={}", parts[3], currentDay);
            return false;
        }

        // 检查月份
        if (!matchesCronField(parts[4], currentMonth)) {
            log.debug("月份不匹配: 期望={}, 实际={}", parts[4], currentMonth);
            return false;
        }

        // 检查星期（Cron中0=Sunday，Java中7=Sunday）
        int cronDayOfWeek = currentDayOfWeek == 7 ? 0 : currentDayOfWeek;
        if (!matchesCronField(parts[5], cronDayOfWeek)) {
            log.debug("星期不匹配: 期望={}, 实际={}", parts[5], cronDayOfWeek);
            return false;
        }

        log.info("Cron表达式匹配成功: {}", cronExpression);
        return true;
    }

    /**
     * 匹配Cron字段
     */
    private boolean matchesCronField(String field, int value) {
        if ("*".equals(field) || "?".equals(field)) {
            return true;
        }

        // 处理数字
        if (field.matches("\\d+")) {
            return Integer.parseInt(field) == value;
        }

        // 处理范围: 1-5
        if (field.matches("\\d+-\\d+")) {
            String[] range = field.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return value >= start && value <= end;
        }

        // 处理步长: */5
        if (field.matches("\\*/\\d+")) {
            int step = Integer.parseInt(field.substring(2));
            return value % step == 0;
        }

        // 处理列表: 1,3,5
        if (field.contains(",")) {
            String[] values = field.split(",");
            for (String v : values) {
                if (Integer.parseInt(v.trim()) == value) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    @Override
    public void scheduleCollectTask(CollectTask task) {
        if (task.getExecuteType() == 2 && task.getCronExpression() != null) {
            String taskKey = "collect_" + task.getId();
            scheduledTasks.put(taskKey, true);
            log.info("注册采集任务定时执行: {}", task.getTaskName());
        }
    }

    @Override
    public void scheduleCompareTask(CompareTask task) {
        if (task.getExecuteType() == 2 && task.getCronExpression() != null) {
            String taskKey = "compare_" + task.getId();
            scheduledTasks.put(taskKey, true);
            log.info("注册比对任务定时执行: {}", task.getTaskName());
        }
    }

    @Override
    public void unscheduleCollectTask(Long taskId) {
        String taskKey = "collect_" + taskId;
        scheduledTasks.remove(taskKey);
        log.info("取消采集任务定时执行: {}", taskId);
    }

    @Override
    public void unscheduleCompareTask(Long taskId) {
        String taskKey = "compare_" + taskId;
        scheduledTasks.remove(taskKey);
        log.info("取消比对任务定时执行: {}", taskId);
    }

    @Override
    public void updateCollectTaskSchedule(CollectTask task) {
        unscheduleCollectTask(task.getId());
        scheduleCollectTask(task);
    }

    @Override
    public void updateCompareTaskSchedule(CompareTask task) {
        unscheduleCompareTask(task.getId());
        scheduleCompareTask(task);
    }

    @Override
    public List<CollectTask> getScheduledCollectTasks() {
        LambdaQueryWrapper<CollectTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CollectTask::getExecuteType, 2) // 定时执行
                   .isNotNull(CollectTask::getCronExpression)
                   .ne(CollectTask::getCronExpression, "")
                   .eq(CollectTask::getStatus, 1); // 启用状态
        return collectTaskService.list(queryWrapper);
    }

    @Override
    public List<CompareTask> getScheduledCompareTasks() {
        LambdaQueryWrapper<CompareTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CompareTask::getExecuteType, 2) // 定时执行
                   .isNotNull(CompareTask::getCronExpression)
                   .ne(CompareTask::getCronExpression, "")
                   .eq(CompareTask::getStatus, 1); // 启用状态
        return compareTaskService.list(queryWrapper);
    }

    @Override
    public boolean validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            return false;
        }

        try {
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length < 6) {
                return false;
            }

            // 验证每个字段的格式
            for (int i = 0; i < parts.length; i++) {
                if (!validateCronField(parts[i], i)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("验证Cron表达式失败: {}", cronExpression, e);
            return false;
        }
    }

    /**
     * 验证Cron字段格式
     */
    private boolean validateCronField(String field, int fieldIndex) {
        if ("*".equals(field) || "?".equals(field)) {
            return true;
        }

        // 数字
        if (field.matches("\\d+")) {
            int value = Integer.parseInt(field);
            return isValidValue(value, fieldIndex);
        }

        // 范围: 1-5
        if (field.matches("\\d+-\\d+")) {
            String[] range = field.split("-");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            return start <= end && isValidValue(start, fieldIndex) && isValidValue(end, fieldIndex);
        }

        // 步长: */5
        if (field.matches("\\*/\\d+")) {
            int step = Integer.parseInt(field.substring(2));
            return step > 0;
        }

        // 列表: 1,3,5
        if (field.contains(",")) {
            String[] values = field.split(",");
            for (String v : values) {
                try {
                    int value = Integer.parseInt(v.trim());
                    if (!isValidValue(value, fieldIndex)) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * 验证字段值是否在有效范围内
     */
    private boolean isValidValue(int value, int fieldIndex) {
        switch (fieldIndex) {
            case 0: // 秒: 0-59
                return value >= 0 && value <= 59;
            case 1: // 分: 0-59
                return value >= 0 && value <= 59;
            case 2: // 时: 0-23
                return value >= 0 && value <= 23;
            case 3: // 日: 1-31
                return value >= 1 && value <= 31;
            case 4: // 月: 1-12
                return value >= 1 && value <= 12;
            case 5: // 周: 0-7 (0=Sunday, 7=Sunday)
                return value >= 0 && value <= 7;
            default:
                return false;
        }
    }

    @Override
    public String getNextExecutionTime(String cronExpression) {
        if (!validateCronExpression(cronExpression)) {
            return "无效的Cron表达式";
        }

        try {
            // 简单的下次执行时间计算（这里简化处理）
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = now.plusMinutes(1); // 假设下次执行在1分钟后
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return next.format(formatter);
        } catch (Exception e) {
            log.error("计算下次执行时间失败: {}", cronExpression, e);
            return "计算失败";
        }
    }
}
