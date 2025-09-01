import request from '@/utils/request'

// 任务调度相关API
export const scheduleApi = {
  // 获取调度任务列表
  getScheduleList(params) {
    return request({
      url: '/schedule/tasks',
      method: 'get',
      params
    })
  },

  // 创建调度任务
  createScheduleTask(data) {
    return request({
      url: '/schedule/tasks',
      method: 'post',
      data
    })
  },

  // 更新调度任务
  updateScheduleTask(data) {
    return request({
      url: `/schedule/tasks/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除调度任务
  deleteScheduleTask(id) {
    return request({
      url: `/schedule/tasks/${id}`,
      method: 'delete'
    })
  },

  // 启用/禁用调度任务
  toggleScheduleStatus(id, status) {
    return request({
      url: `/schedule/tasks/${id}/status`,
      method: 'put',
      data: { status }
    })
  },

  // 立即执行调度任务
  executeScheduleTask(id) {
    return request({
      url: `/schedule/tasks/${id}/execute`,
      method: 'post'
    })
  },

  // 暂停调度任务
  pauseScheduleTask(id) {
    return request({
      url: `/schedule/tasks/${id}/pause`,
      method: 'post'
    })
  },

  // 恢复调度任务
  resumeScheduleTask(id) {
    return request({
      url: `/schedule/tasks/${id}/resume`,
      method: 'post'
    })
  },

  // 获取调度任务执行历史
  getScheduleHistory(taskId, params) {
    return request({
      url: `/schedule/tasks/${taskId}/history`,
      method: 'get',
      params
    })
  },

  // 获取调度任务详情
  getScheduleTaskDetail(id) {
    return request({
      url: `/schedule/tasks/${id}`,
      method: 'get'
    })
  },

  // 批量操作调度任务
  batchScheduleOperation(data) {
    return request({
      url: '/schedule/tasks/batch',
      method: 'post',
      data
    })
  },

  // 获取调度统计信息
  getScheduleStatistics(params) {
    return request({
      url: '/schedule/statistics',
      method: 'get',
      params
    })
  },

  // 验证Cron表达式
  validateCronExpression(cronExpression) {
    return request({
      url: '/schedule/validate-cron',
      method: 'post',
      data: { cronExpression }
    })
  },

  // 获取下次执行时间
  getNextExecutionTime(cronExpression) {
    return request({
      url: '/schedule/next-execution',
      method: 'post',
      data: { cronExpression }
    })
  }
}

export default {
  scheduleApi
}