import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // api的base_url
  timeout: 300000 // 请求超时时间（5分钟，支持AI长时间处理）
})

// request拦截器
service.interceptors.request.use(
  config => {
    // 添加调试日志（避免打印大数据）
    const dataSize = config.data ? JSON.stringify(config.data).length : 0
    console.log('[DEBUG] 请求:', config.method?.toUpperCase(), config.url, dataSize > 500 ? `(数据大小: ${dataSize}字符)` : config.data)
    
    // 在发送请求之前做一些处理
    if (config.method === 'post' || config.method === 'put') {
      // POST和PUT请求时，将参数转换为JSON格式
      if (config.data && typeof config.data === 'object') {
        config.headers['Content-Type'] = 'application/json'
      }
    }
    return config
  },
  error => {
    // 请求错误处理
    console.log('Request Error:', error)
    Promise.reject(error)
  }
)

// response 拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 添加调试日志
    console.log('[DEBUG] 响应拦截器 - 响应数据:', {
      status: response.status,
      url: response.config.url,
      data: res
    })
    
    // 如果返回的状态码为200，说明接口请求成功，可以正常拿到数据
    if (response.status === 200) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  error => {
    console.log('Response Error:', error)
    
    let message = '网络错误'
    if (error.response) {
      const { status, data } = error.response
      switch (status) {
        case 400:
          message = data.message || '请求参数错误'
          break
        case 401:
          message = '未授权，请重新登录'
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址不存在'
          break
        case 500:
          message = data.message || '服务器内部错误'
          break
        default:
          message = data.message || `连接错误${status}`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时'
    }
    
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default service