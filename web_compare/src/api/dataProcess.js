import request from '@/utils/request'

/**
 * 数据处理API
 */
export const dataProcessApi = {
  /**
   * JSON转Excel
   * @param {Object} data - 请求数据
   * @param {string} data.sourceData - 源数据（JSON格式字符串）
   * @param {string} [data.mode='auto'] - 转换模式：auto-自动识别表头，custom-自定义映射
   * @param {string} [data.mapping] - 自定义字段映射（mode=custom时生效）
   * @param {string} [data.fileName='data'] - 导出文件名
   * @param {number} [data.taskId] - 任务ID
   * @param {string} [data.executeId] - 执行ID
   * @returns {Promise}
   */
  convertToExcel(data) {
    return request({
      url: '/data-process/excel/convert',
      method: 'post',
      data
    })
  },

  /**
   * AI智能处理
   * @param {Object} data - 请求数据
   * @param {string} data.sourceData - 源数据（JSON格式字符串）
   * @param {string} data.model - AI模型：deepseek, gpt4, claude3
   * @param {string} data.prompt - 处理指令（Prompt）
   * @param {number} [data.maxTokens=4096] - 最大Token数
   * @param {number} [data.temperature=0.7] - 温度参数
   * @param {number} [data.taskId] - 任务ID
   * @param {string} [data.executeId] - 执行ID
   * @returns {Promise}
   */
  aiProcess(data) {
    return request({
      url: '/data-process/ai/process',
      method: 'post',
      data
    })
  },

  /**
   * 数据清洗
   * @param {Object} data - 请求数据
   * @param {string} data.sourceData - 源数据（JSON格式字符串）
   * @param {Array<string>} data.rules - 清洗规则列表
   *   可选值：remove_null, trim_string, remove_duplicates, format_date
   * @param {string} [data.dateFormat='yyyy-MM-dd HH:mm:ss'] - 目标日期格式
   * @param {number} [data.taskId] - 任务ID
   * @param {string} [data.executeId] - 执行ID
   * @returns {Promise}
   */
  cleanData(data) {
    return request({
      url: '/data-process/clean',
      method: 'post',
      data
    })
  },

  /**
   * 下载处理结果文件
   * @param {string} fileId - 文件ID
   * @returns {string} 下载URL
   */
  getDownloadUrl(fileId) {
    return `/api/data-process/download/${fileId}`
  },

  /**
   * 下载文件（通过创建隐藏链接）
   * @param {string} fileId - 文件ID
   * @param {string} [fileName] - 文件名
   */
  downloadFile(fileId, fileName) {
    const url = this.getDownloadUrl(fileId)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName || 'download'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  },

  /**
   * 获取数据处理任务列表（每个任务关联最新采集结果）
   * @param {Object} params - 查询参数
   * @param {number} params.current - 当前页码
   * @param {number} params.size - 每页大小
   * @param {string} [params.taskName] - 任务名称
   * @param {string} [params.collectType] - 采集类型
   * @param {number} [params.systemId] - 系统ID
   * @returns {Promise}
   */
  getTaskList(params) {
    return request({
      url: '/data-process/tasks',
      method: 'get',
      params
    })
  },

  /**
   * 根据执行ID获取采集结果详情
   * @param {string} executeId - 执行ID
   * @returns {Promise}
   */
  getResultByExecuteId(executeId) {
    return request({
      url: `/data-process/result/${executeId}`,
      method: 'get'
    })
  },

  /**
   * 获取文件信息
   * @param {string} fileId - 文件ID
   * @returns {Promise}
   */
  getFileInfo(fileId) {
    return request({
      url: `/data-process/file/${fileId}`,
      method: 'get'
    })
  },

  /**
   * 清理过期文件
   * @param {number} [expireMinutes=60] - 过期时间（分钟）
   * @returns {Promise}
   */
  cleanExpiredFiles(expireMinutes = 60) {
    return request({
      url: '/data-process/files/expired',
      method: 'delete',
      params: { expireMinutes }
    })
  },

  /**
   * 测试AI连接
   * @param {Object} config - 配置参数
   * @param {string} config.url - API地址
   * @param {string} [config.apiKey] - API Key
   * @param {string} config.modelId - 模型标识
   * @param {number} [config.timeout=30] - 超时时间（秒）
   * @returns {Promise}
   */
  testAiConnection(config) {
    return request({
      url: '/data-process/ai/test-connection',
      method: 'post',
      data: config
    })
  }
}

export default dataProcessApi
