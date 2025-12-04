import request from '@/utils/request'

// 基线管理API
export const baselineApi = {
  // 获取基线列表
  getBaselineList(params) {
    return request({
      url: '/baselines/page',
      method: 'post',
      data: params
    })
  },

  // 根据系统、服务器类型、分类查询基线
  getBaselinesByCondition(systemId, serverTypeId, categoryId) {
    return request({
      url: '/baselines/list',
      method: 'get',
      params: { systemId, serverTypeId, categoryId }
    })
  },

  // 根据系统、分类查询基线
  getBaselinesBySystemAndCategory(systemId, categoryId) {
    return request({
      url: '/baselines/list',
      method: 'get',
      params: { systemId, categoryId }
    })
  },

  // 创建基线
  createBaseline(data) {
    return request({
      url: '/baselines',
      method: 'post',
      data
    })
  },

  // 更新基线
  updateBaseline(data) {
    return request({
      url: `/baselines/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除基线
  deleteBaseline(id) {
    return request({
      url: `/baselines/${id}`,
      method: 'delete'
    })
  },

  // 设置默认基线
  setDefaultBaseline(id, reason = '') {
    return request({
      url: `/baselines/${id}/set-default`,
      method: 'put',
      params: { reason }
    })
  },

  // 获取基线详情
  getBaselineDetail(id) {
    return request({
      url: `/baselines/${id}`,
      method: 'get'
    })
  },

  // 复制基线
  copyBaseline(id, data) {
    return request({
      url: `/baselines/${id}/copy`,
      method: 'post',
      data
    })
  },

  // 导入基线
  importBaseline(data) {
    return request({
      url: '/baselines/import',
      method: 'post',
      data
    })
  },

  // 导出基线
  exportBaseline(id) {
    return request({
      url: `/baselines/${id}/export`,
      method: 'get',
      responseType: 'blob'
    })
  },

  // 获取版本历史
  getVersionHistory(systemId, serverTypeId, categoryId, baselineName) {
    return request({
      url: '/baselines/version-history',
      method: 'get',
      params: { systemId, serverTypeId, categoryId, baselineName }
    })
  },

  // 晋级采集版本为基线
  promoteToBaseline(params) {
    return request({
      url: '/baselines/promote',
      method: 'post',
      params: {
        systemId: params.systemId,
        serverTypeId: params.serverTypeId,
        categoryId: params.categoryId,
        baselineName: params.baselineName,
        currentContent: params.currentContent,
        fileName: params.fileName,
        description: params.description
      }
    })
  },

  // 切换到指定历史版本
  switchToVersion(baselineId, reason) {
    return request({
      url: `/baselines/${baselineId}/switch`,
      method: 'put',
      params: { reason }
    })
  }
}

// 配置分类管理API
export const categoryApi = {
  // 获取配置分类列表（启用的）
  getCategoryList(params) {
    return request({
      url: '/categories/enabled',
      method: 'get',
      params
    })
  },

  // 获取所有配置分类列表（管理页面用）
  getAllCategoryList(params) {
    return request({
      url: '/categories',
      method: 'get',
      params
    })
  },

  // 分页查询配置分类（支持筛选）
  getCategoryPage(params) {
    return request({
      url: '/categories/page',
      method: 'post',
      data: params
    })
  },

  // 根据服务器类型获取适用的配置分类
  getCategoriesByServerType(serverTypeId) {
    return request({
      url: `/categories/by-server-type/${serverTypeId}`,
      method: 'get'
    })
  },

  // 根据系统ID获取适用的配置分类
  getCategoriesBySystem(systemId) {
    return request({
      url: `/categories/by-system/${systemId}`,
      method: 'get'
    })
  },

  // 创建配置分类
  createCategory(data) {
    return request({
      url: '/categories',
      method: 'post',
      data
    })
  },

  // 更新配置分类
  updateCategory(data) {
    return request({
      url: `/categories/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除配置分类
  deleteCategory(id) {
    return request({
      url: `/categories/${id}`,
      method: 'delete'
    })
  }
}

// 基线版本日志API
export const baselineLogApi = {
  // 获取基线版本切换日志
  getVersionLogs(params) {
    return request({
      url: '/baseline-logs',
      method: 'get',
      params
    })
  },

  // 获取指定基线的版本历史
  getBaselineHistory(baselineId) {
    return request({
      url: `/baseline-logs/baseline/${baselineId}`,
      method: 'get'
    })
  }
}
