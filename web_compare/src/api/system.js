import request from '@/utils/request'

// 系统信息管理API
export const systemApi = {
  // 获取系统列表
  getSystemList(params) {
    return request({
      url: '/systems/list',
      method: 'get',
      params
    })
  },

  // 创建系统
  createSystem(data) {
    return request({
      url: '/systems',
      method: 'post',
      data
    })
  },

  // 更新系统
  updateSystem(data) {
    return request({
      url: `/systems/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除系统
  deleteSystem(id) {
    return request({
      url: `/systems/${id}`,
      method: 'delete'
    })
  },

  // 根据ID获取系统详情
  getSystemById(id) {
    return request({
      url: `/systems/${id}`,
      method: 'get'
    })
  },

  // 测试系统连接
  testConnection(id) {
    return request({
      url: `/systems/${id}/test-connection`,
      method: 'post'
    })
  },

  // 根据系统ID获取服务器类型列表
  getServerTypesBySystem(systemId) {
    return request({
      url: `/systems/${systemId}/server-types`,
      method: 'get'
    })
  },

  // 根据系统ID获取服务器实例列表
  getServerInstancesBySystem(params) {
    return request({
      url: `/systems/${params.systemId}/servers`,
      method: 'get',
      params: {
        serverTypeIds: params.serverTypeIds
      }
    })
  }
}

// 服务器类型管理API
export const serverTypeApi = {
  // 获取服务器类型列表
  getServerTypeList(params) {
    return request({
      url: '/server-types/enabled',
      method: 'get',
      params
    })
  },

  // 创建服务器类型
  createServerType(data) {
    return request({
      url: '/server-types',
      method: 'post',
      data
    })
  },

  // 更新服务器类型
  updateServerType(data) {
    return request({
      url: `/server-types/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除服务器类型
  deleteServerType(id) {
    return request({
      url: `/server-types/${id}`,
      method: 'delete'
    })
  }
}

// 服务器实例管理API
export const serverInstanceApi = {
  // 获取服务器实例列表
  getServerInstanceList(params) {
    return request({
      url: '/servers/page',
      method: 'post',
      data: params
    })
  },

  // 创建服务器实例
  createServerInstance(data) {
    return request({
      url: '/servers',
      method: 'post',
      data
    })
  },

  // 更新服务器实例
  updateServerInstance(data) {
    return request({
      url: `/servers/${data.id}`,
      method: 'put',
      data
    })
  },

  // 删除服务器实例
  deleteServerInstance(id) {
    return request({
      url: `/servers/${id}`,
      method: 'delete'
    })
  },

  // 测试服务器连接
  testServerConnection(id) {
    return request({
      url: `/servers/${id}/test-connection`,
      method: 'post'
    })
  },

  // 批量测试服务器连接
  batchTestConnection(ids) {
    return request({
      url: '/servers/batch-test',
      method: 'post',
      data: { serverIds: ids }
    })
  },

  // 根据系统ID和类型ID获取服务器列表
  getServersBySystemAndType(systemId, typeId) {
    return request({
      url: `/systems/${systemId}/servers`,
      method: 'get',
      params: { serverTypeIds: typeId }
    })
  }
}