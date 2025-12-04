import request from '@/utils/request'

// 外部链接管理API
export const externalLinkApi = {
  // 获取外部链接列表（分页）
  getExternalLinkList(params) {
    return request({
      url: '/external-links',
      method: 'get',
      params
    })
  },

  // 获取所有启用的外部链接（不分页，用于菜单渲染）
  getEnabledLinks() {
    return request({
      url: '/external-links/enabled',
      method: 'get'
    })
  },

  // 根据ID获取外部链接详情
  getExternalLinkById(id) {
    return request({
      url: `/external-links/${id}`,
      method: 'get'
    })
  },

  // 创建外部链接
  createExternalLink(data) {
    return request({
      url: '/external-links',
      method: 'post',
      data
    })
  },

  // 更新外部链接
  updateExternalLink(data) {
    return request({
      url: `/external-links/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除外部链接
  deleteExternalLink(id) {
    return request({
      url: `/external-links/${id}`,
      method: 'delete'
    })
  },

  // 批量删除外部链接
  batchDeleteExternalLinks(ids) {
    return request({
      url: '/external-links/batch',
      method: 'delete',
      data: { ids }
    })
  },

  // 更新外部链接状态
  updateExternalLinkStatus(id, status) {
    return request({
      url: `/external-links/${id}/status`,
      method: 'put',
      data: { status }
    })
  },

  // 更新排序
  updateSortOrder(id, sortOrder) {
    return request({
      url: `/external-links/${id}/sort`,
      method: 'put',
      data: { sortOrder }
    })
  }
}
