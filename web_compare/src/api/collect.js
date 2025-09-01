import request from '@/utils/request'

// 采集模板相关API
export const collectTemplateApi = {
  // 获取采集模板列表
  getTemplateList(params) {
    return request({
      url: '/collect/templates',
      method: 'get',
      params
    })
  },

  // 创建采集模板
  createTemplate(data) {
    return request({
      url: '/collect/templates',
      method: 'post',
      data
    })
  },

  // 更新采集模板
  updateTemplate(data) {
    return request({
      url: `/collect/templates/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除采集模板
  deleteTemplate(id) {
    return request({
      url: `/collect/templates/${id}`,
      method: 'delete'
    })
  },

  // 根据ID获取采集模板详情
  getTemplateById(id) {
    return request({
      url: `/collect/templates/${id}`,
      method: 'get'
    })
  },

  // 测试采集模板
  testTemplate(id) {
    return request({
      url: `/collect/templates/${id}/test`,
      method: 'post'
    })
  },

  // 测试模板连接
  testTemplateConnection(data) {
    const { templateId, ...requestData } = data
    return request({
      url: `/collect/templates/${templateId}/test`,
      method: 'post',
      data: requestData
    })
  },

  // 根据服务器类型获取适用模板
  getTemplatesByServerType(serverTypeId) {
    return request({
      url: `/collect/templates/by-server-type/${serverTypeId}`,
      method: 'get'
    })
  }
}

// 采集任务相关API
export const collectTaskApi = {
  // 获取采集任务列表
  getTaskList(params) {
    return request({
      url: '/collect/tasks',
      method: 'get',
      params
    })
  },

  // 创建采集任务
  createTask(data) {
    return request({
      url: '/collect/tasks',
      method: 'post',
      data
    })
  },

  // 更新采集任务
  updateTask(data) {
    return request({
      url: `/collect/tasks/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除采集任务
  deleteTask(id) {
    return request({
      url: `/collect/tasks/${id}`,
      method: 'delete'
    })
  },

  // 根据ID获取采集任务详情
  getTaskById(id) {
    return request({
      url: `/collect/tasks/${id}`,
      method: 'get'
    })
  },

  // 启用/禁用采集任务
  toggleTaskStatus(id, status) {
    return request({
      url: `/collect/tasks/${id}/status`,
      method: 'put',
      data: { status }
    })
  },

  // 立即执行采集任务
  executeTask(id) {
    return request({
      url: `/collect/tasks/${id}/execute`,
      method: 'post'
    })
  },

  // 批量执行采集任务
  batchExecute(taskIds) {
    return request({
      url: '/collect/tasks/batch-execute',
      method: 'post',
      data: { taskIds }
    })
  },

  // 暂停采集任务
  pauseTask(id) {
    return request({
      url: `/collect/tasks/${id}/pause`,
      method: 'post'
    })
  },

  // 恢复采集任务
  resumeTask(id) {
    return request({
      url: `/collect/tasks/${id}/resume`,
      method: 'post'
    })
  },

  // 获取任务执行历史
  getTaskExecutions(taskId, params) {
    return request({
      url: `/collect/tasks/${taskId}/executions`,
      method: 'get',
      params
    })
  },

  // 获取任务执行结果
  getTaskResults(taskId, executionId, params) {
    return request({
      url: `/collect/tasks/${taskId}/executions/${executionId}/results`,
      method: 'get',
      params
    })
  }
}

// 采集执行相关API
export const collectExecutionApi = {
  // 获取采集执行列表
  getExecutionList(params) {
    return request({
      url: '/collect/executions',
      method: 'get',
      params
    })
  },

  // 根据ID获取执行详情
  getExecutionById(id) {
    return request({
      url: `/collect/executions/${id}`,
      method: 'get'
    })
  },

  // 停止执行
  stopExecution(id) {
    return request({
      url: `/collect/executions/${id}/stop`,
      method: 'post'
    })
  },

  // 重新执行
  retryExecution(id) {
    return request({
      url: `/collect/executions/${id}/retry`,
      method: 'post'
    })
  }
}

export default {
  collectTemplateApi,
  collectTaskApi,
  collectExecutionApi
}