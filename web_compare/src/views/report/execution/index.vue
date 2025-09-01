<template>
  <div class="task-execution">
    <div class="page-header">
      <h2 class="page-title">任务执行报告</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleGenerateReport">
          <el-icon><Document /></el-icon>
          生成报告
        </el-button>
        <el-button type="success" @click="handleExportReport">
          <el-icon><Download /></el-icon>
          导出报告
        </el-button>
        <el-button type="info" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div class="overview-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon total">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">总执行次数</div>
              <div class="card-value">{{ overview.totalExecutions || 0 }}</div>
              <div class="card-desc">近七天</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon success">
              <el-icon><CircleCheck /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">成功执行</div>
              <div class="card-value">{{ overview.successExecutions || 0 }}</div>
              <div class="card-desc">成功率: {{ overview.successRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon failed">
              <el-icon><CircleClose /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">失败执行</div>
              <div class="card-value">{{ overview.failedExecutions || 0 }}</div>
              <div class="card-desc">失败率: {{ overview.failureRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon duration">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">平均耗时</div>
              <div class="card-value">{{ formatDuration(overview.averageDuration) }}</div>
              <div class="card-desc">最短: {{ formatDuration(overview.minDuration) }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 搜索栏 -->
    <div class="app-card">
      <el-form :model="searchForm" label-width="80px" :inline="true">
        <el-form-item label="任务名称">
          <el-input
            v-model="searchForm.taskName"
            placeholder="请输入任务名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select
            v-model="searchForm.taskType"
            placeholder="请选择任务类型"
            clearable
            style="width: 150px"
          >
            <el-option label="采集任务" value="COLLECT" />
            <el-option label="比对任务" value="COMPARE" />
            <el-option label="调度任务" value="SCHEDULE" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 120px"
          >
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已取消" value="CANCELLED" />
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

    <!-- 数据表格 -->
    <div class="app-card">
      <el-table
        v-loading="loading"
        :data="executionList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="执行ID" width="100" />
        <el-table-column prop="taskName" label="任务名称" min-width="150" />
        <el-table-column prop="taskType" label="任务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTaskTypeColor(row.taskType)" size="small">
              {{ getTaskTypeText(row.taskType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="执行状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="duration" label="执行耗时" width="120">
          <template #default="{ row }">
            {{ formatExecutionDuration(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功/总数" width="120">
          <template #default="{ row }">
            {{ row.successCount || 0 }}/{{ row.totalCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
            <el-button type="info" size="small" @click="handleViewLogs(row)">
              查看日志
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
import { ElMessage } from 'element-plus'
import { executionReportApi } from '@/api/report'

export default {
  name: 'TaskExecution',
  setup() {
    // 响应式数据
    const loading = ref(false)
    const executionList = ref([])
    const overview = ref({})
    
    // 搜索表单
    const searchForm = reactive({
      taskName: '',
      taskType: '',
      status: ''
    })
    
    // 分页
    const pagination = reactive({
      current: 1,
      size: 20,
      total: 0
    })
    
    // 获取执行列表
    const getExecutionList = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        
        const response = await executionReportApi.getExecutionReports(params)
        executionList.value = response.records || []
        pagination.total = response.total || 0
      } catch (error) {
        console.error('获取执行列表失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 获取概览数据
    const getOverview = async () => {
      try {
        const response = await executionReportApi.getExecutionOverview()
        overview.value = response.data || {}
      } catch (error) {
        console.error('获取概览数据失败:', error)
        ElMessage.error('获取概览数据失败')
      }
    }
    
    // 事件处理函数
    const handleSearch = () => {
      pagination.current = 1
      getExecutionList()
    }
    
    const handleReset = () => {
      Object.assign(searchForm, {
        taskName: '',
        taskType: '',
        status: ''
      })
      handleSearch()
    }
    
    const handleRefresh = () => {
      getExecutionList()
      getOverview()
    }
    
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      getExecutionList()
    }
    
    const handleCurrentChange = (current) => {
      pagination.current = current
      getExecutionList()
    }
    
    const handleViewDetail = (row) => {
      ElMessage.info('查看详情功能开发中...')
    }
    
    const handleViewLogs = (row) => {
      ElMessage.info('查看日志功能开发中...')
    }
    
    const handleGenerateReport = () => {
      ElMessage.info('生成报告功能开发中...')
    }
    
    const handleExportReport = () => {
      ElMessage.info('导出报告功能开发中...')
    }
    
    // 工具函数
    const getTaskTypeColor = (type) => {
      const colorMap = {
        'COLLECT': 'primary',
        'COMPARE': 'success',
        'SCHEDULE': 'warning'
      }
      return colorMap[type] || 'info'
    }
    
    const getTaskTypeText = (type) => {
      const textMap = {
        'COLLECT': '采集',
        'COMPARE': '比对',
        'SCHEDULE': '调度'
      }
      return textMap[type] || '未知'
    }
    
    const getStatusColor = (status) => {
      const colorMap = {
        'SUCCESS': 'success',
        'FAILED': 'danger',
        'RUNNING': 'warning',
        'CANCELLED': 'info'
      }
      return colorMap[status] || 'info'
    }
    
    const getStatusText = (status) => {
      const textMap = {
        'SUCCESS': '成功',
        'FAILED': '失败',
        'RUNNING': '运行中',
        'CANCELLED': '已取消'
      }
      return textMap[status] || '未知'
    }
    
    const formatDuration = (ms) => {
      if (!ms) return '-'
      if (ms < 1000) return `${ms}ms`
      if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
      return `${(ms / 60000).toFixed(1)}min`
    }
    
    const formatExecutionDuration = (startTime, endTime) => {
      if (!startTime || !endTime) return '-'
      const start = new Date(startTime)
      const end = new Date(endTime)
      const duration = end - start
      return formatDuration(duration)
    }
    
    // 初始化
    onMounted(async () => {
      await Promise.all([
        getExecutionList(),
        getOverview()
      ])
    })
    
    return {
      loading,
      executionList,
      overview,
      searchForm,
      pagination,
      handleSearch,
      handleReset,
      handleRefresh,
      handleSizeChange,
      handleCurrentChange,
      handleViewDetail,
      handleViewLogs,
      handleGenerateReport,
      handleExportReport,
      getTaskTypeColor,
      getTaskTypeText,
      getStatusColor,
      getStatusText,
      formatDuration,
      formatExecutionDuration
    }
  }
}
</script>

<style lang="scss" scoped>
.task-execution {
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
  
  .overview-section {
    margin-bottom: 20px;
    
    .overview-card {
      background: #fff;
      border-radius: 8px;
      padding: 20px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
      display: flex;
      align-items: center;
      
      .card-icon {
        width: 50px;
        height: 50px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 20px;
        color: #fff;
        margin-right: 15px;
        
        &.total { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
        &.success { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
        &.failed { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
        &.duration { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
      }
      
      .card-content {
        flex: 1;
        
        .card-title {
          font-size: 14px;
          color: #909399;
          margin-bottom: 5px;
        }
        
        .card-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 5px;
        }
        
        .card-desc {
          font-size: 12px;
          color: #909399;
        }
      }
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
}
</style>