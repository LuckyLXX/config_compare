import request from '@/utils/request'

// 报告仪表板相关API
export const reportDashboardApi = {
  // 获取仪表板概览数据
  getDashboardOverview(params) {
    return request({
      url: '/report/dashboard/overview',
      method: 'get',
      params
    })
  },

  // 获取系统状态统计
  getSystemStats(params) {
    return request({
      url: '/report/dashboard/system-stats',
      method: 'get',
      params
    })
  },

  // 获取任务执行趋势
  getTaskTrends(params) {
    return request({
      url: '/report/dashboard/task-trends',
      method: 'get',
      params
    })
  },

  // 获取比对结果分布
  getCompareDistribution(params) {
    return request({
      url: '/report/dashboard/compare-distribution',
      method: 'get',
      params
    })
  },

  // 获取最近执行记录
  getRecentExecutions(params) {
    return request({
      url: '/report/dashboard/recent-executions',
      method: 'get',
      params
    })
  },

  // 获取告警信息
  getAlerts(params) {
    return request({
      url: '/report/dashboard/alerts',
      method: 'get',
      params
    })
  }
}

// 任务执行报告相关API
export const executionReportApi = {
  // 获取执行概览数据
  getExecutionOverview() {
    return request({
      url: '/report/executions/overview',
      method: 'get'
    })
  },

  // 获取执行报告列表
  getExecutionReports(params) {
    return request({
      url: '/report/executions',
      method: 'get',
      params
    })
  },

  // 生成执行报告
  generateExecutionReport(data) {
    return request({
      url: '/report/executions/generate',
      method: 'post',
      data
    })
  },

  // 导出执行报告
  exportExecutionReport(params) {
    return request({
      url: '/report/executions/export',
      method: 'post',
      data: params,
      responseType: 'blob'
    })
  }
}



// 比对报告相关API
export const compareReportApi = {
  // 导出比对报告（Excel）
  exportCompareExcel(data) {
    return request({
      url: '/report/compare/export-excel',
        method: 'post',
        headers: { 'Content-Type': 'application/json;charset=UTF-8' },
        data: JSON.stringify(data),
      responseType: 'blob'
    })
  },

  // 获取报告详情数据
  getReportData(systemId) {
    return request({
      url: `/report/compare/data/${systemId}`,
      method: 'get'
    })
  }
}

export default {
  reportDashboardApi,
  executionReportApi,
  compareReportApi
}
