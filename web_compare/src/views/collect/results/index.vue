<template>
  <div class="collect-results">
    <div class="page-header">
      <h2 class="page-title">采集结果</h2>
      <div class="page-actions">
        <el-button @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回历史
        </el-button>
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="success" @click="handleExport" :disabled="resultList.length === 0">
          <el-icon><Download /></el-icon>
          导出结果
        </el-button>
      </div>
    </div>

    <!-- 执行信息卡片 -->
    <div class="app-card" v-if="executionInfo">
      <div class="execution-info">
        <div class="info-row">
          <div class="info-item">
            <label>任务名称：</label>
            <span>{{ executionInfo.taskName }}</span>
          </div>
          <div class="info-item">
            <label>执行ID：</label>
            <span class="execute-id">{{ executionInfo.executeId }}</span>
          </div>
          <div class="info-item">
            <label>执行状态：</label>
            <el-tag 
              :type="getExecuteStatusType(executionInfo.executeStatus)"
              :icon="getExecuteStatusIcon(executionInfo.executeStatus)"
            >
              {{ getExecuteStatusText(executionInfo.executeStatus) }}
            </el-tag>
          </div>
        </div>
        <div class="info-row">
          <div class="info-item">
            <label>开始时间：</label>
            <span>{{ formatDateTime(executionInfo.startTime) }}</span>
          </div>
          <div class="info-item">
            <label>结束时间：</label>
            <span>{{ executionInfo.endTime ? formatDateTime(executionInfo.endTime) : '进行中' }}</span>
          </div>
          <div class="info-item">
            <label>执行耗时：</label>
            <span>{{ formatDuration(executionInfo.durationMs) }}</span>
          </div>
        </div>
        <div class="info-row">
          <div class="info-item">
            <label>服务器统计：</label>
            <div class="server-summary">
              <span class="success">成功 {{ executionInfo.successServers }}</span>
              <span class="failed" v-if="executionInfo.failedServers > 0">失败 {{ executionInfo.failedServers }}</span>
              <span class="total">总计 {{ executionInfo.totalServers }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="app-card">
      <el-form :model="searchForm" label-width="80px" :inline="true">
        <el-form-item label="服务器">
          <el-select
            v-model="searchForm.serverInstanceId"
            placeholder="请选择服务器"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="server in serverList"
              :key="server.id"
              :label="server.serverName"
              :value="server.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="采集状态">
          <el-select
            v-model="searchForm.collectStatus"
            placeholder="请选择状态"
            clearable
            style="width: 120px"
          >
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 结果列表 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="resultList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="serverName" label="服务器" width="150" />
        <el-table-column prop="collectItemName" label="采集项" width="150" />
        <el-table-column prop="collectType" label="采集类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.collectType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="collectStatus" label="采集状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.collectStatus === 1 ? 'success' : 'danger'" size="small">
              {{ row.collectStatus === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="executeTime" label="执行时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.executeTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="耗时" width="80">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="200">
          <template #default="{ row }">
            <div v-if="row.errorMessage" class="error-message">
              <el-tooltip :content="row.errorMessage" placement="top">
                <span class="truncate">{{ row.errorMessage }}</span>
              </el-tooltip>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleViewContent(row)"
              :disabled="row.collectStatus !== 1"
            >
              查看内容
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        style="margin-top: 20px; text-align: right"
      />
    </div>

    <!-- 内容查看对话框 -->
    <el-dialog
      v-model="contentDialogVisible"
      :title="`采集内容 - ${selectedResult?.serverName} - ${selectedResult?.collectItemName}`"
      width="80%"
      :close-on-click-modal="false"
    >
      <div class="content-viewer">
        <div class="content-header">
          <div class="content-info">
            <span>类型：{{ selectedResult?.collectType }}</span>
            <span>状态：{{ selectedResult?.collectStatus === 1 ? '成功' : '失败' }}</span>
            <span>时间：{{ formatDateTime(selectedResult?.executeTime) }}</span>
          </div>
          <div class="content-actions">
            <el-button size="small" @click="handleCopyContent">
              <el-icon><DocumentCopy /></el-icon>
              复制
            </el-button>
            <el-button size="small" @click="handleDownloadContent">
              <el-icon><Download /></el-icon>
              下载
            </el-button>
          </div>
        </div>
        <div class="content-body">
          <el-input
            v-model="contentText"
            type="textarea"
            :rows="20"
            readonly
            placeholder="暂无内容"
          />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { collectTaskApi, collectExecutionApi } from '@/api/collect'

export default {
  name: 'CollectResults',
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    // 响应式数据
    const loading = ref(false)
    const resultList = ref([])
    const serverList = ref([])
    const executionInfo = ref(null)
    const contentDialogVisible = ref(false)
    const selectedResult = ref(null)
    const contentText = ref('')
    
    // 搜索表单
    const searchForm = reactive({
      serverInstanceId: null,
      collectStatus: null
    })
    
    // 分页
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // 获取参数
    const taskId = route.query.taskId
    const executeId = route.query.executeId
    
    // 获取执行信息
    const getExecutionInfo = async () => {
      if (!taskId || !executeId) return
      
      try {
        const resp = await collectExecutionApi.getExecutionById(executeId)
        executionInfo.value = resp.data || resp
      } catch (error) {
        console.error('获取执行信息失败:', error)
        ElMessage.error('获取执行信息失败')
      }
    }
    
    // 获取结果列表
    const getResultList = async () => {
      if (!taskId || !executeId) {
        ElMessage.error('缺少必要参数')
        return
      }
      
      loading.value = true
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        const response = await collectTaskApi.getTaskResults(taskId, executeId, params)
        const data = response.data || response
        resultList.value = data.records || []
        pagination.total = data.total || 0

        // 生成服务器下拉数据
        const map = new Map()
        resultList.value.forEach(r => {
          if (r.serverInstanceId && !map.has(r.serverInstanceId)) {
            map.set(r.serverInstanceId, {
              id: r.serverInstanceId,
              serverName: r.serverName || `${r.serverInstanceId}`
            })
          }
        })
        serverList.value = Array.from(map.values())
      } catch (error) {
        console.error('获取采集结果失败:', error)
        ElMessage.error('获取采集结果失败')
      } finally {
        loading.value = false
      }
    }
    
    // 事件处理函数
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getResultList()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      getResultList()
    }
    
    const handleSearch = () => {
      pagination.current = 1
      getResultList()
    }
    
    const handleReset = () => {
      Object.assign(searchForm, {
        serverInstanceId: null,
        collectStatus: null
      })
      handleSearch()
    }
    
    const handleBack = () => {
      router.push(`/collect/executions?taskId=${taskId}`)
    }
    
    const handleRefresh = () => {
      getResultList()
    }
    
    const handleExport = () => {
      ElMessage.info('导出功能开发中...')
    }
    
    const handleViewContent = (row) => {
      selectedResult.value = row
      contentText.value = row.collectContent || '暂无内容'
      contentDialogVisible.value = true
    }
    
    const handleCopyContent = async () => {
      try {
        await navigator.clipboard.writeText(contentText.value)
        ElMessage.success('内容已复制到剪贴板')
      } catch (error) {
        ElMessage.error('复制失败')
      }
    }
    
    const handleDownloadContent = () => {
      if (!selectedResult.value) return
      
      const content = contentText.value
      const filename = `${selectedResult.value.serverName}_${selectedResult.value.collectItemName}_${new Date().getTime()}.txt`
      
      const blob = new Blob([content], { type: 'text/plain' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename
      link.click()
      window.URL.revokeObjectURL(url)
    }
    
    // 状态相关函数
    const getExecuteStatusType = (status) => {
      const statusMap = {
        1: 'success',    // 成功
        2: 'warning',    // 部分成功
        3: 'danger',     // 失败
        4: 'info'        // 运行中
      }
      return statusMap[status] || ''
    }
    
    const getExecuteStatusIcon = (status) => {
      const iconMap = {
        1: 'Check',      // 成功
        2: 'Warning',    // 部分成功
        3: 'Close',      // 失败
        4: 'Loading'     // 运行中
      }
      return iconMap[status] || ''
    }
    
    const getExecuteStatusText = (status) => {
      const textMap = {
        1: '成功',
        2: '部分成功',
        3: '失败',
        4: '运行中'
      }
      return textMap[status] || '未知'
    }
    
    // 时间格式化函数
    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      return new Date(dateTime).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
    
    // 耗时格式化函数
    const formatDuration = (ms) => {
      if (!ms) return '-'
      
      if (ms < 1000) {
        return `${ms}ms`
      } else if (ms < 60000) {
        return `${Math.round(ms / 1000)}s`
      } else {
        const minutes = Math.floor(ms / 60000)
        const seconds = Math.round((ms % 60000) / 1000)
        return `${minutes}m${seconds}s`
      }
    }
    
    // 初始化
    onMounted(async () => {
      await Promise.all([
        getExecutionInfo(),
        getResultList()
      ])
    })
    
    return {
      loading,
      resultList,
      serverList,
      executionInfo,
      contentDialogVisible,
      selectedResult,
      contentText,
      searchForm,
      pagination,
      handleSizeChange,
      handleCurrentChange,
      handleSearch,
      handleReset,
      handleBack,
      handleRefresh,
      handleExport,
      handleViewContent,
      handleCopyContent,
      handleDownloadContent,
      getExecuteStatusType,
      getExecuteStatusIcon,
      getExecuteStatusText,
      formatDateTime,
      formatDuration
    }
  }
}
</script>

<style lang="scss" scoped>
.collect-results {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: 500;
    }
    
    .page-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .app-card {
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
    padding: 20px;
    margin-bottom: 20px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .execution-info {
    .info-row {
      display: flex;
      gap: 40px;
      margin-bottom: 12px;
      
      &:last-child {
        margin-bottom: 0;
      }
    }
    
    .info-item {
      display: flex;
      align-items: center;
      
      label {
        font-weight: 500;
        color: #666;
        margin-right: 8px;
        white-space: nowrap;
      }
      
      .execute-id {
        font-family: monospace;
        background: #f5f5f5;
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 12px;
      }
      
      .server-summary {
        display: flex;
        gap: 12px;
        
        .success {
          color: #67c23a;
        }
        
        .failed {
          color: #f56c6c;
        }
        
        .total {
          color: #409eff;
        }
      }
    }
  }
  
  .error-message {
    .truncate {
      display: inline-block;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: middle;
    }
  }
  
  .content-viewer {
    .content-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #eee;
      
      .content-info {
        display: flex;
        gap: 16px;
        color: #666;
        font-size: 14px;
      }
      
      .content-actions {
        display: flex;
        gap: 8px;
      }
    }
    
    .content-body {
      :deep(.el-textarea__inner) {
        font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
        font-size: 12px;
        line-height: 1.4;
      }
    }
  }
}
</style>
