<template>
  <div class="task-statistics">
    <div class="page-header">
      <h2 class="page-title">任务统计分析</h2>
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
        <el-button type="primary" @click="handleExport">
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
          <div class="overview-card collect">
            <div class="card-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">采集任务</div>
              <div class="card-value">{{ overview.collectTasks || 0 }}</div>
              <div class="card-desc">成功率: {{ overview.collectSuccessRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card compare">
            <div class="card-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">比对任务</div>
              <div class="card-value">{{ overview.compareTasks || 0 }}</div>
              <div class="card-desc">成功率: {{ overview.compareSuccessRate || 0 }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card execution">
            <div class="card-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">总执行次数</div>
              <div class="card-value">{{ overview.totalExecutions || 0 }}</div>
              <div class="card-desc">平均耗时: {{ overview.avgDuration || 0 }}s</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="overview-card error">
            <div class="card-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-title">失败任务</div>
              <div class="card-value">{{ overview.failedTasks || 0 }}</div>
              <div class="card-desc">失败率: {{ overview.failureRate || 0 }}%</div>
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
            <el-radio-group v-model="trendPeriod" size="small" @change="loadTrendData">
              <el-radio-button label="7d">7天</el-radio-button>
              <el-radio-button label="30d">30天</el-radio-button>
              <el-radio-button label="90d">90天</el-radio-button>
            </el-radio-group>
          </div>
          <div ref="trendChart" style="height: 350px"></div>
        </div>
      </el-col>

      <!-- 任务类型分布 -->
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>任务类型分布</h3>
          </div>
          <div ref="typeChart" style="height: 350px"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 系统执行统计 -->
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>系统执行统计</h3>
          </div>
          <div ref="systemChart" style="height: 350px"></div>
        </div>
      </el-col>

      <!-- 执行时间分布 -->
      <el-col :span="12">
        <div class="app-card">
          <div class="card-header">
            <h3>执行时间分布</h3>
          </div>
          <div ref="durationChart" style="height: 350px"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 详细统计表格 -->
    <div class="app-card">
      <div class="card-header">
        <h3>详细统计</h3>
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="采集统计" name="collect">
            <el-table :data="collectStats" stripe v-loading="loading">
              <el-table-column prop="systemName" label="系统名称" width="150" />
              <el-table-column prop="totalTasks" label="总任务数" width="100" />
              <el-table-column prop="successTasks" label="成功数" width="100" />
              <el-table-column prop="failedTasks" label="失败数" width="100" />
              <el-table-column prop="successRate" label="成功率" width="100">
                <template #default="{ row }">
                  {{ row.successRate }}%
                </template>
              </el-table-column>
              <el-table-column prop="avgDuration" label="平均耗时" width="120">
                <template #default="{ row }">
                  {{ row.avgDuration }}s
                </template>
              </el-table-column>
              <el-table-column prop="lastExecuteTime" label="最后执行时间" width="180" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="viewDetail(row, 'collect')">
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          
          <el-tab-pane label="比对统计" name="compare">
            <el-table :data="compareStats" stripe v-loading="loading">
              <el-table-column prop="systemName" label="系统名称" width="150" />
              <el-table-column prop="totalTasks" label="总任务数" width="100" />
              <el-table-column prop="consistentTasks" label="一致数" width="100" />
              <el-table-column prop="inconsistentTasks" label="不一致数" width="100" />
              <el-table-column prop="failedTasks" label="失败数" width="100" />
              <el-table-column prop="consistencyRate" label="一致率" width="100">
                <template #default="{ row }">
                  {{ row.consistencyRate }}%
                </template>
              </el-table-column>
              <el-table-column prop="avgDuration" label="平均耗时" width="120">
                <template #default="{ row }">
                  {{ row.avgDuration }}s
                </template>
              </el-table-column>
              <el-table-column prop="lastExecuteTime" label="最后执行时间" width="180" />
              <el-table-column label="操作" width="120">
                <template #default="{ row }">
                  <el-button type="primary" size="small" @click="viewDetail(row, 'compare')">
                    查看详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Download,
  Refresh,
  Timer
} from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'
import * as echarts from 'echarts'

export default {
  name: 'TaskStatistics',
  components: {
    Download,
    Refresh,
    Timer
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const dateRange = ref([])
    const trendPeriod = ref('7d')
    const activeTab = ref('collect')
    
    // 概览数据
    const overview = reactive({
      collectTasks: 1246,
      collectSuccessRate: 92.8,
      compareTasks: 856,
      compareSuccessRate: 88.5,
      totalExecutions: 2102,
      avgDuration: 45.6,
      failedTasks: 90,
      failureRate: 4.3
    })
    
    // 统计数据
    const collectStats = ref([])
    const compareStats = ref([])
    
    // 图表引用
    const trendChart = ref(null)
    const typeChart = ref(null)
    const systemChart = ref(null)
    const durationChart = ref(null)
    
    let trendChartInstance = null
    let typeChartInstance = null
    let systemChartInstance = null
    let durationChartInstance = null
    
    // 初始化日期范围（最近7天）
    const initDateRange = () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 7)
      dateRange.value = [start, end]
    }
    
    // 加载概览数据
    const loadOverview = async () => {
      try {
        // 这里可以调用实际的API
        console.log('加载概览数据')
      } catch (error) {
        console.error('获取概览数据失败:', error)
      }
    }
    
    // 加载采集统计
    const loadCollectStats = async () => {
      loading.value = true
      try {
        // 模拟数据
        collectStats.value = [
          { systemName: '生产环境', totalTasks: 156, successTasks: 145, failedTasks: 11, successRate: 92.9, avgDuration: 42.5, lastExecuteTime: '2024-01-15 10:30:00' },
          { systemName: 'UAT环境', totalTasks: 89, successTasks: 82, failedTasks: 7, successRate: 92.1, avgDuration: 38.2, lastExecuteTime: '2024-01-15 10:25:00' },
          { systemName: '测试环境', totalTasks: 234, successTasks: 218, failedTasks: 16, successRate: 93.2, avgDuration: 35.8, lastExecuteTime: '2024-01-15 10:20:00' }
        ]
      } catch (error) {
        console.error('获取采集统计失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 加载比对统计
    const loadCompareStats = async () => {
      loading.value = true
      try {
        // 模拟数据
        compareStats.value = [
          { systemName: '生产环境', totalTasks: 124, consistentTasks: 108, inconsistentTasks: 12, failedTasks: 4, consistencyRate: 87.1, avgDuration: 52.3, lastExecuteTime: '2024-01-15 10:30:00' },
          { systemName: 'UAT环境', totalTasks: 76, consistentTasks: 68, inconsistentTasks: 6, failedTasks: 2, consistencyRate: 89.5, avgDuration: 48.7, lastExecuteTime: '2024-01-15 10:25:00' },
          { systemName: '测试环境', totalTasks: 198, consistentTasks: 175, inconsistentTasks: 18, failedTasks: 5, consistencyRate: 88.4, avgDuration: 41.2, lastExecuteTime: '2024-01-15 10:20:00' }
        ]
      } catch (error) {
        console.error('获取比对统计失败:', error)
      } finally {
        loading.value = false
      }
    }
    
    // 初始化图表
    const initCharts = async () => {
      await nextTick()
      
      // 任务执行趋势图
      if (trendChart.value) {
        trendChartInstance = echarts.init(trendChart.value)
        renderTrendChart()
      }
      
      // 任务类型分布图
      if (typeChart.value) {
        typeChartInstance = echarts.init(typeChart.value)
        renderTypeChart()
      }
      
      // 系统执行统计图
      if (systemChart.value) {
        systemChartInstance = echarts.init(systemChart.value)
        renderSystemChart()
      }
      
      // 执行时间分布图
      if (durationChart.value) {
        durationChartInstance = echarts.init(durationChart.value)
        renderDurationChart()
      }
      
      // 窗口大小改变时重新调整图表
      window.addEventListener('resize', () => {
        trendChartInstance?.resize()
        typeChartInstance?.resize()
        systemChartInstance?.resize()
        durationChartInstance?.resize()
      })
    }
    
    // 渲染趋势图
    const renderTrendChart = () => {
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'cross' }
        },
        legend: {
          data: ['采集任务', '比对任务']
        },
        xAxis: {
          type: 'category',
          data: ['01-09', '01-10', '01-11', '01-12', '01-13', '01-14', '01-15']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '采集任务',
            type: 'line',
            data: [45, 52, 38, 48, 56, 42, 48],
            smooth: true,
            itemStyle: { color: '#409eff' }
          },
          {
            name: '比对任务',
            type: 'line',
            data: [32, 38, 28, 35, 42, 31, 36],
            smooth: true,
            itemStyle: { color: '#67c23a' }
          }
        ]
      }
      trendChartInstance.setOption(option)
    }
    
    // 渲染类型分布图
    const renderTypeChart = () => {
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
            name: '任务类型',
            type: 'pie',
            radius: '50%',
            data: [
              { value: 1246, name: '采集任务', itemStyle: { color: '#409eff' } },
              { value: 856, name: '比对任务', itemStyle: { color: '#67c23a' } },
              { value: 234, name: '调度任务', itemStyle: { color: '#e6a23c' } }
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
      typeChartInstance.setOption(option)
    }
    
    // 渲染系统统计图
    const renderSystemChart = () => {
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        legend: {
          data: ['采集成功', '采集失败', '比对成功', '比对失败']
        },
        xAxis: {
          type: 'category',
          data: ['生产环境', 'UAT环境', '测试环境']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '采集成功',
            type: 'bar',
            stack: '采集',
            data: [145, 82, 218],
            itemStyle: { color: '#409eff' }
          },
          {
            name: '采集失败',
            type: 'bar',
            stack: '采集',
            data: [11, 7, 16],
            itemStyle: { color: '#f56c6c' }
          },
          {
            name: '比对成功',
            type: 'bar',
            stack: '比对',
            data: [108, 68, 175],
            itemStyle: { color: '#67c23a' }
          },
          {
            name: '比对失败',
            type: 'bar',
            stack: '比对',
            data: [16, 8, 23],
            itemStyle: { color: '#e6a23c' }
          }
        ]
      }
      systemChartInstance.setOption(option)
    }
    
    // 渲染执行时间分布图
    const renderDurationChart = () => {
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        xAxis: {
          type: 'category',
          data: ['0-10s', '10-30s', '30-60s', '60-120s', '120s+']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '任务数量',
            type: 'bar',
            data: [156, 423, 678, 234, 89],
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ])
            }
          }
        ]
      }
      durationChartInstance.setOption(option)
    }
    
    // 事件处理
    const handleDateRangeChange = () => {
      loadOverview()
      loadTrendData()
    }
    
    const handleRefresh = () => {
      loadOverview()
      loadCollectStats()
      loadCompareStats()
      loadTrendData()
    }
    
    const handleExport = () => {
      ElMessage.info('导出功能开发中...')
    }
    
    const handleTabChange = (tab) => {
      if (tab === 'collect' && collectStats.value.length === 0) {
        loadCollectStats()
      } else if (tab === 'compare' && compareStats.value.length === 0) {
        loadCompareStats()
      }
    }
    
    const loadTrendData = () => {
      renderTrendChart()
    }
    
    const viewDetail = (row, type) => {
      ElMessage.info(`查看${type === 'collect' ? '采集' : '比对'}详情功能开发中...`)
    }
    
    // 初始化
    onMounted(async () => {
      initDateRange()
      await initCharts()
      loadOverview()
      loadCollectStats()
    })
    
    // 组件卸载
    onUnmounted(() => {
      trendChartInstance?.dispose()
      typeChartInstance?.dispose()
      systemChartInstance?.dispose()
      durationChartInstance?.dispose()
      window.removeEventListener('resize', () => {})
    })
    
    return {
      loading,
      dateRange,
      trendPeriod,
      activeTab,
      overview,
      collectStats,
      compareStats,
      trendChart,
      typeChart,
      systemChart,
      durationChart,
      handleDateRangeChange,
      handleRefresh,
      handleExport,
      handleTabChange,
      loadTrendData,
      viewDetail
    }
  }
}
</script>

<style lang="scss" scoped>
.task-statistics {
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
      
      &.collect .card-icon { background: linear-gradient(135deg, #409eff 0%, #69c0ff 100%); }
      &.compare .card-icon { background: linear-gradient(135deg, #67c23a 0%, #95de64 100%); }
      &.execution .card-icon { background: linear-gradient(135deg, #e6a23c 0%, #ffa940 100%); }
      &.error .card-icon { background: linear-gradient(135deg, #f56c6c 0%, #ff7875 100%); }
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
}
</style>
