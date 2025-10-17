<template>
  <div class="report-dashboard">
    <div class="page-header">
      <h2 class="page-title">报告中心仪表板</h2>
      <div class="page-actions">
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          @change="handleDateRangeChange"
          style="width: 350px; margin-right: 10px"
        />
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 概览统计 -->
    <div class="overview-section">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon system">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">系统数量</div>
              <div class="card-value">{{ overview.systemCount || 0 }}</div>
              <div class="card-desc">在线: {{ overview.onlineSystemCount || 0 }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon task">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">任务数量</div>
              <div class="card-value">{{ overview.taskCount || 0 }}</div>
              <div class="card-desc">运行中: {{ overview.runningTaskCount || 0 }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon execution">
              <el-icon><VideoPlay /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">今日执行</div>
              <div class="card-value">{{ overview.todayExecutionCount || 0 }}</div>
              <div class="card-desc">成功率: {{ overview.todaySuccessRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card">
            <div class="card-icon compare">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">比对结果</div>
              <div class="card-value">{{ overview.compareResultCount || 0 }}</div>
              <div class="card-desc">一致性: {{ overview.consistencyRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <!-- 任务执行趋势 -->
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>任务执行趋势</h3>
            <el-radio-group v-model="trendPeriod" size="small" @change="loadTaskTrends">
              <el-radio-button label="7d">7天</el-radio-button>
              <el-radio-button label="30d">30天</el-radio-button>
              <el-radio-button label="90d">90天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="taskTrendChart" style="height: 300px"></div>
        </div>
      </el-col>

      <!-- 比对结果分布 -->
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>比对结果分布</h3>
          </div>
          <div ref="compareDistributionChart" style="height: 300px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 系统状态统计 -->
      <el-col :span="8">
        <div class="app-card">
          <div class="card-header">
            <h3>系统状态</h3>
          </div>
          <div class="system-stats">
            <div 
              v-for="stat in systemStats" 
              :key="stat.systemName"
              class="system-stat-item"
            >
              <div class="system-name">{{ stat.systemName }}</div>
              <div class="system-status">
                <el-tag :type="getSystemStatusType(stat.status)" size="small">
                  {{ getSystemStatusText(stat.status) }}
                </el-tag>
              </div>
              <div class="system-metrics">
                <span>采集: {{ stat.collectCount || 0 }}</span>
                <span>比对: {{ stat.compareCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 最近执行记录 -->
      <el-col :span="16">
        <div class="app-card">
          <div class="card-header">
            <h3>最近执行记录</h3>
            <div>
              <el-switch v-model="showAbnormalOnly" active-text="仅看异常" style="margin-right: 10px" />
              <el-button type="text" @click="viewAllExecutions">查看全部</el-button>
            </div>
          </div>
          <el-table :data="filteredRecentExecutions" stripe size="small">
            <el-table-column prop="taskName" label="任务名称" min-width="120" />
            <el-table-column prop="taskType" label="类型" width="80">
              <template #default="{ row }">
                <el-tag :type="row.taskType === 'COLLECT' ? 'primary' : 'success'" size="small">
                  {{ row.taskType === 'COLLECT' ? '采集' : '比对' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="getExecutionStatusType(row.status)" size="small">
                  {{ getExecutionStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="executeTime" label="开始时间" width="150" />
            <el-table-column prop="duration" label="耗时" width="80" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="text" size="small" @click="viewExecutionDetail(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>

    <!-- 告警信息 -->
    <div class="app-card" v-if="alerts.length > 0">
      <div class="card-header">
        <h3>告警信息</h3>
        <el-button type="text" @click="clearAllAlerts">清除全部</el-button>
      </div>
      <div class="alerts-container">
        <el-alert
          v-for="alert in alerts"
          :key="alert.id"
          :title="alert.title"
          :description="alert.description"
          :type="alert.type"
          :closable="true"
          @close="removeAlert(alert.id)"
          style="margin-bottom: 10px"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { reportDashboardApi } from '@/api/report'

export default {
  name: 'ReportDashboard',
  setup() {
    const router = useRouter()
    
    // 响应式数据
    const dateRange = ref([])
    const trendPeriod = ref('7d')
    const overview = ref({})
    const systemStats = ref([])
    const recentExecutions = ref([])
    const alerts = ref([])
    
    // 图表实例
    const taskTrendChart = ref(null)
    const compareDistributionChart = ref(null)
    let taskTrendChartInstance = null
    let compareDistributionChartInstance = null
    
    // 初始化日期范围（最近7天）
    const initDateRange = () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      dateRange.value = [start, end]
    }
    
    // 获取概览数据
    const loadOverview = async () => {
      try {
        const params = getDateRangeParams()
        console.log('🔍 请求概览数据参数:', params)
        const response = await reportDashboardApi.getDashboardOverview(params)
        console.log('🔍 概览数据API响应:', response)
        console.log('🔍 概览数据响应类型:', typeof response)
        console.log('🔍 概览数据响应结构:', JSON.stringify(response, null, 2))
        
        overview.value = response?.data || {}
        console.log('🔍 设置后的overview.value:', overview.value)
        console.log('🔍 overview.value.systemCount:', overview.value.systemCount)
        console.log('🔍 overview.value.taskCount:', overview.value.taskCount)
      } catch (error) {
        console.error('获取概览数据失败:', error)
      }
    }
    
    // 获取系统状态统计
    const loadSystemStats = async () => {
      try {
        const response = await reportDashboardApi.getSystemStats()
        console.log('🔍 系统状态统计API响应:', response)
        console.log('🔍 系统状态统计响应结构:', JSON.stringify(response, null, 2))
        systemStats.value = response?.data?.records || []
        console.log('🔍 设置后的systemStats.value:', systemStats.value)
      } catch (error) {
        console.error('获取系统状态统计失败:', error)
      }
    }
    
    // 获取最近执行记录
    const loadRecentExecutions = async () => {
      try {
        const response = await reportDashboardApi.getRecentExecutions({ size: 10 })
        console.log('🔍 最近执行记录API响应:', response)
        console.log('🔍 最近执行记录响应结构:', JSON.stringify(response, null, 2))
        recentExecutions.value = response?.data?.records || []
        console.log('🔍 设置后的recentExecutions.value:', recentExecutions.value)
      } catch (error) {
        console.error('获取最近执行记录失败:', error)
      }
    }
    
    // 获取告警信息
    const loadAlerts = async () => {
      try {
        const response = await reportDashboardApi.getAlerts()
        alerts.value = response?.data?.records || []
      } catch (error) {
        console.error('获取告警信息失败:', error)
      }
    }
    
    // 获取任务执行趋势数据
    const loadTaskTrends = async () => {
      try {
        const params = {
          period: trendPeriod.value,
          ...getDateRangeParams()
        }
        console.log('🔍 请求任务趋势数据参数:', params)
        const response = await reportDashboardApi.getTaskTrends(params)
        console.log('🔍 任务趋势数据API响应:', response)
        console.log('🔍 任务趋势数据响应结构:', JSON.stringify(response, null, 2))
        renderTaskTrendChart(response?.data || {})
      } catch (error) {
        console.error('获取任务趋势数据失败:', error)
      }
    }
    
    // 获取比对结果分布数据
    const loadCompareDistribution = async () => {
      try {
        const params = getDateRangeParams()
        console.log('🔍 请求比对结果分布参数:', params)
        const response = await reportDashboardApi.getCompareDistribution(params)
        console.log('🔍 比对结果分布API响应:', response)
        console.log('🔍 比对结果分布响应结构:', JSON.stringify(response, null, 2))
        renderCompareDistributionChart(response?.data || {})
      } catch (error) {
        console.error('获取比对结果分布数据失败:', error)
      }
    }
    
    // 渲染任务趋势图表
    const renderTaskTrendChart = (data) => {
      if (!taskTrendChartInstance) return
      
      console.log('🔍 渲染任务趋势图表数据:', data)
      console.log('🔍 图表数据dates:', data.dates)
      console.log('🔍 图表数据collectCounts:', data.collectCounts)
      console.log('🔍 图表数据compareCounts:', data.compareCounts)
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'cross'
          }
        },
        legend: {
          data: ['采集任务', '比对任务']
        },
        xAxis: {
          type: 'category',
          data: data.dates || []
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '采集任务',
            type: 'line',
            data: data.collectCounts || [],
            smooth: true,
            itemStyle: { color: '#409eff' }
          },
          {
            name: '比对任务',
            type: 'line',
            data: data.compareCounts || [],
            smooth: true,
            itemStyle: { color: '#67c23a' }
          }
        ]
      }
      
      taskTrendChartInstance.setOption(option)
    }
    
    // 渲染比对结果分布图表
    const renderCompareDistributionChart = (data) => {
      if (!compareDistributionChartInstance) return
      
      console.log('🔍 渲染比对结果分布图表数据:', data)
      console.log('🔍 分布数据consistent:', data.consistent)
      console.log('🔍 分布数据inconsistent:', data.inconsistent)
      console.log('🔍 分布数据failed:', data.failed)
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{a} <br/>{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '比对结果',
            type: 'pie',
            radius: '50%',
            data: [
              { value: data.consistent || 0, name: '一致', itemStyle: { color: '#67c23a' } },
              { value: data.inconsistent || 0, name: '不一致', itemStyle: { color: '#e6a23c' } },
              { value: data.failed || 0, name: '失败', itemStyle: { color: '#f56c6c' } }
            ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
      
      compareDistributionChartInstance.setOption(option)
    }
    
    // 初始化图表
    const initCharts = async () => {
      await nextTick()
      
      if (taskTrendChart.value) {
        taskTrendChartInstance = echarts.init(taskTrendChart.value)
      }
      
      if (compareDistributionChart.value) {
        compareDistributionChartInstance = echarts.init(compareDistributionChart.value)
      }
      
      // 窗口大小改变时重新调整图表
      window.addEventListener('resize', () => {
        taskTrendChartInstance?.resize()
        compareDistributionChartInstance?.resize()
      })
    }
    
    // 获取日期范围参数
    const getDateRangeParams = () => {
      if (dateRange.value && dateRange.value.length === 2) {
        return {
          startDate: dateRange.value[0],
          endDate: dateRange.value[1]
        }
      }
      return {}
    }
    
    // 事件处理函数
    const handleDateRangeChange = () => {
      loadAll()
    }
    
    const handleRefresh = () => {
      loadAll()
    }
    
    const viewAllExecutions = () => {
      router.push('/report/execution')
    }
    
    const viewExecutionDetail = (row) => {
      router.push(`/report/execution/${row.id}`)
    }
    
    const removeAlert = (alertId) => {
      const index = alerts.value.findIndex(alert => alert.id === alertId)
      if (index > -1) {
        alerts.value.splice(index, 1)
      }
    }
    
    const clearAllAlerts = () => {
      alerts.value = []
      ElMessage.success('已清除所有告警')
    }
    
    // 工具函数
    const getSystemStatusType = (status) => {
      const statusMap = {
        'ONLINE': 'success',
        'OFFLINE': 'danger',
        'WARNING': 'warning'
      }
      return statusMap[status] || 'info'
    }
    
    const getSystemStatusText = (status) => {
      const textMap = {
        'ONLINE': '在线',
        'OFFLINE': '离线',
        'WARNING': '警告'
      }
      return textMap[status] || '未知'
    }
    
    const getExecutionStatusType = (status) => {
      const statusMap = {
        'SUCCESS': 'success',
        'PARTIAL': 'warning',
        'FAILED': 'danger',
        'RUNNING': 'warning',
        'CANCELLED': 'info'
      }
      return statusMap[status] || 'info'
    }
    
    const getExecutionStatusText = (status) => {
      const textMap = {
        'SUCCESS': '成功',
        'PARTIAL': '部分成功',
        'FAILED': '失败',
        'RUNNING': '运行中',
        'CANCELLED': '已取消'
      }
      return textMap[status] || '未知'
    }
    
    // 加载所有数据
    const loadAll = async () => {
      await Promise.all([
        loadOverview(),
        loadSystemStats(),
        loadRecentExecutions(),
        loadAlerts(),
        loadTaskTrends(),
        loadCompareDistribution()
      ])
    }
    
    // 组件挂载
    onMounted(async () => {
      console.log('🔍 页面加载开始')
      initDateRange()
      console.log('🔍 初始化日期范围完成:', dateRange.value)
      await initCharts()
      console.log('🔍 初始化图表完成')
      await loadAll()
      console.log('🔍 加载所有数据完成')
    })
    
    // 组件卸载
    onUnmounted(() => {
      taskTrendChartInstance?.dispose()
      compareDistributionChartInstance?.dispose()
      window.removeEventListener('resize', () => {})
    })
    
    const showAbnormalOnly = ref(false)
    const filteredRecentExecutions = computed(() => {
      if (!showAbnormalOnly.value) return recentExecutions.value
      return (recentExecutions.value || []).filter(r => r.status === 'FAILED' || r.status === 'PARTIAL')
    })

    return {
      dateRange,
      trendPeriod,
      overview,
      systemStats,
      recentExecutions,
      filteredRecentExecutions,
      showAbnormalOnly,
      alerts,
      taskTrendChart,
      compareDistributionChart,
      handleDateRangeChange,
      handleRefresh,
      viewAllExecutions,
      viewExecutionDetail,
      removeAlert,
      clearAllAlerts,
      getSystemStatusType,
      getSystemStatusText,
      getExecutionStatusType,
      getExecutionStatusText,
      loadTaskTrends
    }
  }
}
</script>

<style lang="scss" scoped>
.report-dashboard {
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
      align-items: center;
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
        width: 60px;
        height: 60px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        color: #fff;
        margin-right: 15px;
        
        &.system { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
        &.task { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        &.execution { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        &.compare { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
      }
      
      .card-content {
        flex: 1;
        
        .card-title {
          font-size: 14px;
          color: #909399;
          margin-bottom: 5px;
        }
        
        .card-value {
          font-size: 28px;
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
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      
      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
      }
    }
  }
  
  .system-stats {
    .system-stat-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 0;
      border-bottom: 1px solid #f0f0f0;
      
      &:last-child {
        border-bottom: none;
      }
      
      .system-name {
        font-weight: 500;
        flex: 1;
      }
      
      .system-status {
        margin: 0 10px;
      }
      
      .system-metrics {
        display: flex;
        gap: 10px;
        font-size: 12px;
        color: #909399;
        
        span {
          white-space: nowrap;
        }
      }
    }
  }
  
  .alerts-container {
    max-height: 300px;
    overflow-y: auto;
  }
}
</style>