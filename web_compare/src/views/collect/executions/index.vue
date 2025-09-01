<template>
  <div class="collect-executions">
    <div class="page-header">
      <h2 class="page-title">执行历史</h2>
      <div class="page-actions">
        <el-button @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回任务列表
        </el-button>
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 任务信息卡片 -->
    <div class="app-card" v-if="taskInfo">
      <div class="task-info">
        <div class="task-name">{{ taskInfo.taskName }}</div>
        <div class="task-details">
          <el-tag>{{ taskInfo.systemName }}</el-tag>
          <el-tag type="info">{{ taskInfo.templateName }}</el-tag>
          <el-tag :type="taskInfo.executeType === 1 ? 'warning' : 'info'">
            {{ taskInfo.executeType === 1 ? '立即执行' : '定时执行' }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 执行历史列表 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="executionList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="executeId" label="执行ID" width="180" />
        <el-table-column prop="executeStatus" label="执行状态" width="120">
          <template #default="{ row }">
            <el-tag 
              :type="getExecuteStatusType(row.executeStatus)"
              :icon="getExecuteStatusIcon(row.executeStatus)"
            >
              {{ getExecuteStatusText(row.executeStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="服务器统计" width="150">
          <template #default="{ row }">
            <div class="server-stats">
              <div class="stat-item success">
                <span class="count">{{ row.successServers }}</span>
                <span class="label">成功</span>
              </div>
              <div class="stat-item failed" v-if="row.failedServers > 0">
                <span class="count">{{ row.failedServers }}</span>
                <span class="label">失败</span>
              </div>
              <div class="stat-item total">
                <span class="count">{{ row.totalServers }}</span>
                <span class="label">总计</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="160">
          <template #default="{ row }">
            {{ row.endTime ? formatDateTime(row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="durationMs" label="执行耗时" width="100">
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="handleViewResults(row)"
              :disabled="row.executeStatus === 4"
            >
              查看结果
            </el-button>
            <el-button 
              type="info" 
              size="small" 
              @click="handleViewDetails(row)"
            >
              详情
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
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { collectTaskApi } from '@/api/collect'

export default {
  name: 'CollectExecutions',
  setup() {
    const route = useRoute()
    const router = useRouter()
    
    // 响应式数据
    const loading = ref(false)
    const executionList = ref([])
    const taskInfo = ref(null)
    
    // 分页
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // 获取任务ID
    const taskId = route.query.taskId
    
    // 获取任务信息
    const getTaskInfo = async () => {
      if (!taskId) return
      
      try {
        const response = await collectTaskApi.getTaskById(taskId)
        const data = response.data || response
        taskInfo.value = data
      } catch (error) {
        console.error('获取任务信息失败:', error)
        ElMessage.error('获取任务信息失败')
      }
    }
    
    // 获取执行历史列表
    const getExecutionList = async () => {
      if (!taskId) {
        ElMessage.error('缺少任务ID参数')
        return
      }
      
      loading.value = true
      try {
        const response = await collectTaskApi.getTaskExecutions(taskId, {
          current: pagination.current,
          size: pagination.size
        })
        const data = response.data || response
        executionList.value = data.records || []
        pagination.total = data.total || 0
      } catch (error) {
        console.error('获取执行历史失败:', error)
        ElMessage.error('获取执行历史失败')
      } finally {
        loading.value = false
      }
    }
    
    // 事件处理函数
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getExecutionList()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      getExecutionList()
    }
    
    const handleBack = () => {
      router.push('/collect/tasks')
    }
    
    const handleRefresh = () => {
      getExecutionList()
    }
    
    const handleViewResults = (row) => {
      window.open(`/collect/results?taskId=${taskId}&executeId=${row.executeId}`, '_blank')
    }
    
    const handleViewDetails = (row) => {
      // TODO: 显示执行详情对话框
      ElMessage.info('执行详情功能开发中...')
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
        getTaskInfo(),
        getExecutionList()
      ])
    })
    
    return {
      loading,
      executionList,
      taskInfo,
      pagination,
      handleSizeChange,
      handleCurrentChange,
      handleBack,
      handleRefresh,
      handleViewResults,
      handleViewDetails,
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
.collect-executions {
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
  
  .task-info {
    .task-name {
      font-size: 18px;
      font-weight: 500;
      margin-bottom: 10px;
    }
    
    .task-details {
      display: flex;
      gap: 8px;
    }
  }
  
  .server-stats {
    display: flex;
    gap: 8px;
    
    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      
      .count {
        font-weight: bold;
        font-size: 14px;
      }
      
      .label {
        font-size: 12px;
        color: #666;
      }
      
      &.success .count {
        color: #67c23a;
      }
      
      &.failed .count {
        color: #f56c6c;
      }
      
      &.total .count {
        color: #409eff;
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
}
</style>
