import request from '@/utils/request'

// 仪表板API
export const dashboardApi = {
  // 获取仪表板统计数据
  getStats() {
    return request({
      url: '/dashboard/stats',
      method: 'get'
    })
  }
}
