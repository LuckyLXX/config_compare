import request from '@/utils/request'

// 比对任务相关API
export const compareTaskApi = {
  // 获取比对任务列表
  getTaskList(params) {
    return request({
      url: '/compare/tasks',
      method: 'get',
      params
    })
  },

  // 创建比对任务
  createTask(data) {
    return request({
      url: '/compare/tasks',
      method: 'post',
      data
    })
  },

  // 更新比对任务
  updateTask(data) {
    return request({
      url: `/compare/tasks/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除比对任务
  deleteTask(id) {
    return request({
      url: `/compare/tasks/${id}`,
      method: 'delete'
    })
  },

  // 根据ID获取比对任务详情
  getTaskById(id) {
    return request({
      url: `/compare/tasks/${id}`,
      method: 'get'
    })
  },

  // 启用/禁用比对任务
  toggleTaskStatus(id, status) {
    return request({
      url: `/compare/tasks/${id}/status`,
      method: 'put',
      data: { status }
    })
  },

  // 立即执行比对任务
  executeTask(id) {
    return request({
      url: `/compare/tasks/${id}/execute`,
      method: 'post'
    })
  },

  // 批量执行比对任务
  batchExecute(taskIds) {
    return request({
      url: '/compare/tasks/batch-execute',
      method: 'post',
      data: { taskIds }
    })
  },

  // 暂停比对任务
  pauseTask(id) {
    return request({
      url: `/compare/tasks/${id}/pause`,
      method: 'post'
    })
  },

  // 恢复比对任务
  resumeTask(id) {
    return request({
      url: `/compare/tasks/${id}/resume`,
      method: 'post'
    })
  },

  // 获取任务执行历史
  getTaskExecutions(taskId, params) {
    return request({
      url: `/compare/tasks/${taskId}/executions`,
      method: 'get',
      params
    })
  }
}

// 比对结果相关API
export const compareResultApi = {
  // 获取比对结果列表
  getResultList(params) {
    return request({
      url: '/compare/results',
      method: 'get',
      params
    })
  },

  // 根据ID获取比对结果详情
  getResultById(id) {
    return request({
      url: `/compare/results/${id}`,
      method: 'get'
    })
  },

  // 根据任务ID获取比对结果
  getResultsByTaskId(taskId, params) {
    return request({
      url: `/compare/tasks/${taskId}/results`,
      method: 'get',
      params
    })
  },

  // 根据执行ID获取比对结果
  getResultsByExecuteId(executeId, params) {
    return request({
      url: `/compare/executions/${executeId}/results`,
      method: 'get',
      params
    })
  },

  // 获取差异详情
  getDiffDetails(resultId, params) {
    return request({
      url: `/compare/results/${resultId}/diffs`,
      method: 'get',
      params
    })
  },

  // 导出比对结果
  exportResults(params) {
    return request({
      url: '/compare/results/export',
      method: 'post',
      data: params,
      responseType: 'blob'
    })
  },

  // 获取结果统计
  getResultStatistics(params) {
    return request({
      url: '/compare/results/statistics',
      method: 'get',
      params
    })
  }
}

// 比对执行相关API
export const compareExecutionApi = {
  // 获取比对执行列表
  getExecutionList(params) {
    return request({
      url: '/compare/executions',
      method: 'get',
      params
    })
  },

  // 根据ID获取执行详情
  getExecutionById(id) {
    return request({
      url: `/compare/executions/${id}`,
      method: 'get'
    })
  },

  // 停止执行
  stopExecution(id) {
    return request({
      url: `/compare/executions/${id}/stop`,
      method: 'post'
    })
  },

  // 重新执行
  retryExecution(id) {
    return request({
      url: `/compare/executions/${id}/retry`,
      method: 'post'
    })
  }
}

export default {
  compareTaskApi,
  compareResultApi,
  compareExecutionApi
}