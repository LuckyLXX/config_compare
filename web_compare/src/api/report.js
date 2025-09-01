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

// 差异分析报告相关API
export const diffAnalysisApi = {
  // 获取差异分析报告
  getDiffAnalysisReports(params) {
    return request({
      url: '/report/diff-analysis',
      method: 'get',
      params
    })
  },

  // 生成差异分析报告
  generateDiffAnalysisReport(data) {
    return request({
      url: '/report/diff-analysis/generate',
      method: 'post',
      data
    })
  },

  // 获取差异趋势分析
  getDiffTrends(params) {
    return request({
      url: '/report/diff-analysis/trends',
      method: 'get',
      params
    })
  }
}

// 系统健康报告相关API
export const systemHealthApi = {
  // 获取系统健康报告
  getSystemHealthReports(params) {
    return request({
      url: '/report/system-health',
      method: 'get',
      params
    })
  },

  // 获取系统健康指标
  getSystemHealthMetrics(params) {
    return request({
      url: '/report/system-health/metrics',
      method: 'get',
      params
    })
  },

  // 获取性能指标
  getPerformanceMetrics(params) {
    return request({
      url: '/report/system-health/performance',
      method: 'get',
      params
    })
  }
}

// 统计报告相关API
export const statisticsApi = {
  // 获取采集统计
  getCollectStatistics(params) {
    return request({
      url: '/report/statistics/collect',
      method: 'get',
      params
    })
  },

  // 获取比对统计
  getCompareStatistics(params) {
    return request({
      url: '/report/statistics/compare',
      method: 'get',
      params
    })
  },

  // 获取系统使用统计
  getUsageStatistics(params) {
    return request({
      url: '/report/statistics/usage',
      method: 'get',
      params
    })
  }
}

export default {
  reportDashboardApi,
  executionReportApi,
  diffAnalysisApi,
  systemHealthApi,
  statisticsApi
}