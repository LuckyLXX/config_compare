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

  aiProcessStream(data, handlers = {}, options = {}) {
    const { onDelta, onEnd, onError } = handlers
    const { signal } = options

    return fetch('/api/data-process/ai/process/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data),
      signal
    }).then(async (resp) => {
      if (!resp.ok) {
        const text = await resp.text().catch(() => '')
        throw new Error(text || `HTTP ${resp.status}`)
      }

      if (!resp.body) {
        throw new Error('当前浏览器不支持流式读取')
      }

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      let eventName = 'message'
      let dataLines = []

      const dispatch = () => {
        const dataText = dataLines.join('\n')
        if (eventName === 'delta') {
          onDelta && onDelta(dataText)
        } else if (eventName === 'end') {
          onEnd && onEnd(dataText)
        } else if (eventName === 'error') {
          onError && onError(dataText)
        }
        eventName = 'message'
        dataLines = []
      }

      const feedLine = (line) => {
        // SSE 标准：空行表示一个事件结束
        if (line === '') {
          if (dataLines.length > 0 || eventName !== 'message') {
            dispatch()
          }
          return
        }

        // 注释行
        if (line.startsWith(':')) return

        if (line.startsWith('event:')) {
          eventName = line.slice('event:'.length).trim()
          return
        }

        if (line.startsWith('data:')) {
          // SSE 规范：data: 后可能有一个可选空格
          let v = line.slice('data:'.length)
          if (v.startsWith(' ')) v = v.slice(1)
          dataLines.push(v)
          return
        }

        // 兼容非标准实现：data: 后面的续行没有 data: 前缀
        if (dataLines.length > 0) {
          dataLines.push(line)
        }
      }

      while (true) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        let idx
        while ((idx = buffer.indexOf('\n')) !== -1) {
          let line = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 1)
          if (line.endsWith('\r')) line = line.slice(0, -1)
          feedLine(line)
        }
      }

      // 处理尾部残留
      if (buffer.length > 0) {
        let line = buffer
        if (line.endsWith('\r')) line = line.slice(0, -1)
        feedLine(line)
      }
      // 如果没有以空行结尾，仍然尝试派发一次
      if (dataLines.length > 0 || eventName !== 'message') {
        dispatch()
      }
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
