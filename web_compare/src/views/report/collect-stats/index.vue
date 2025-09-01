<template>
  <div class="collect-stats">
    <div class="page-header">
      <h2 class="page-title">采集统计报告</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleGenerateReport">
          <el-icon><Document /></el-icon>
          生成统计报告
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

    <!-- 筛选条件 -->
    <div class="app-card">
      <el-form :model="searchForm" label-width="100px" :inline="true">
        <el-form-item label="时间范围:">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 350px"
          />
        </el-form-item>
        <el-form-item label="系统:">
          <el-select v-model="searchForm.systemId" placeholder="请选择系统" clearable style="width: 200px">
            <el-option
              v-for="item in systemOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="环境:">
          <el-select v-model="searchForm.environment" placeholder="请选择环境" clearable style="width: 150px">
            <el-option label="开发环境" value="dev" />
            <el-option label="测试环境" value="test" />
            <el-option label="预生产" value="pre" />
            <el-option label="生产环境" value="prod" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务状态:">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 150px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
            <el-option label="执行中" value="running" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计概览 -->
    <div class="app-card">
      <h3 class="card-title">采集任务概览</h3>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card stat-total">
            <div class="stat-icon">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.totalTasks }}</div>
              <div class="stat-label">总任务数</div>
              <div class="stat-change">+{{ overview.taskChange }} 较上周</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-success">
            <div class="stat-icon">
              <el-icon><SuccessFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.successTasks }}</div>
              <div class="stat-label">成功任务</div>
              <div class="stat-rate">成功率: {{ overview.successRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-failed">
            <div class="stat-icon">
              <el-icon><CircleCloseFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.failedTasks }}</div>
              <div class="stat-label">失败任务</div>
              <div class="stat-rate">失败率: {{ overview.failedRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-avg-time">
            <div class="stat-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.avgExecutionTime }}s</div>
              <div class="stat-label">平均执行时间</div>
              <div class="stat-change">{{ overview.timeChange }}s 较上周</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 统计图表 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">采集任务执行趋势</h3>
          <div class="chart-container">
            <div ref="trendChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">任务状态分布</h3>
          <div class="chart-container">
            <div ref="statusChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">系统采集量分布</h3>
          <div class="chart-container">
            <div ref="systemChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">执行时间分析</h3>
          <div class="chart-container">
            <div ref="timeChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- TOP 采集任务 -->
    <div class="app-card">
      <h3 class="card-title">TOP 采集任务</h3>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="执行次数最多" name="most-executed">
          <el-table :data="topTasks.mostExecuted" stripe style="width: 100%">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="taskName" label="任务名称" width="200" />
            <el-table-column prop="systemName" label="系统" width="150" />
            <el-table-column prop="executionCount" label="执行次数" width="120" />
            <el-table-column prop="successRate" label="成功率" width="100">
              <template #default="{ row }">
                <el-progress :percentage="row.successRate" :status="getProgressStatus(row.successRate)" :stroke-width="6" />
              </template>
            </el-table-column>
            <el-table-column prop="avgTime" label="平均时间" width="120" />
            <el-table-column prop="lastExecuted" label="最后执行" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="执行时间最长" name="longest-time">
          <el-table :data="topTasks.longestTime" stripe style="width: 100%">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="taskName" label="任务名称" width="200" />
            <el-table-column prop="systemName" label="系统" width="150" />
            <el-table-column prop="maxTime" label="最长时间" width="120" />
            <el-table-column prop="avgTime" label="平均时间" width="120" />
            <el-table-column prop="executionCount" label="执行次数" width="120" />
            <el-table-column prop="lastExecuted" label="最后执行" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="失败率最高" name="highest-failure">
          <el-table :data="topTasks.highestFailure" stripe style="width: 100%">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="taskName" label="任务名称" width="200" />
            <el-table-column prop="systemName" label="系统" width="150" />
            <el-table-column prop="failureRate" label="失败率" width="120">
              <template #default="{ row }">
                <el-tag :type="getFailureRateType(row.failureRate)">{{ row.failureRate }}%</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="failedCount" label="失败次数" width="120" />
            <el-table-column prop="totalCount" label="总次数" width="120" />
            <el-table-column prop="lastFailed" label="最后失败" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 详细统计数据 -->
    <div class="app-card">
      <h3 class="card-title">详细统计数据</h3>
      <el-table
        :data="detailStats"
        v-loading="loading"
        stripe
        style="width: 100%"
        show-summary
        :summary-method="getSummaries"
      >
        <el-table-column prop="taskName" label="任务名称" width="200" show-overflow-tooltip />
        <el-table-column prop="systemName" label="系统" width="120" />
        <el-table-column prop="environment" label="环境" width="100">
          <template #default="{ row }">
            <el-tag :type="getEnvironmentType(row.environment)">{{ getEnvironmentText(row.environment) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalExecutions" label="执行次数" width="100" />
        <el-table-column prop="successCount" label="成功次数" width="100" />
        <el-table-column prop="failedCount" label="失败次数" width="100" />
        <el-table-column prop="successRate" label="成功率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.successRate" :status="getProgressStatus(row.successRate)" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column prop="avgExecutionTime" label="平均时间" width="120" />
        <el-table-column prop="maxExecutionTime" label="最长时间" width="120" />
        <el-table-column prop="minExecutionTime" label="最短时间" width="120" />
        <el-table-column prop="lastExecuted" label="最后执行" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDetail(row)">
              <el-icon><View /></el-icon>
              查看详情
            </el-button>
            <el-button link type="success" @click="handleAnalyze(row)">
              <el-icon><DataAnalysis /></el-icon>
              分析
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Document,
  Download,
  Refresh,
  Search,
  Collection,
  SuccessFilled,
  CircleCloseFilled,
  Timer,
  View,
  DataAnalysis
} from '@element-plus/icons-vue'
import { statisticsApi } from '@/api/report'
import { systemApi } from '@/api/system'
import * as echarts from 'echarts'

export default {
  name: 'CollectStats',
  components: {
    Document,
    Download,
    Refresh,
    Search,
    Collection,
    SuccessFilled,
    CircleCloseFilled,
    Timer,
    View,
    DataAnalysis
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const systemOptions = ref([])
    
    // 搜索表单
    const searchForm = reactive({
      dateRange: [],
      systemId: '',
      environment: '',
      status: ''
    })

    // 概览数据
    const overview = reactive({
      totalTasks: 1246,
      taskChange: 23,
      successTasks: 1156,
      successRate: 92.8,
      failedTasks: 90,
      failedRate: 7.2,
      avgExecutionTime: 45.6,
      timeChange: -2.3
    })

    // 活跃Tab
    const activeTab = ref('most-executed')

    // TOP 任务数据
    const topTasks = reactive({
      mostExecuted: [],
      longestTime: [],
      highestFailure: []
    })

    // 详细统计数据
    const detailStats = ref([])
    
    // 分页
    const pagination = reactive({
      page: 1,
      size: 20,
      total: 0
    })

    // 图表参考
    const trendChart = ref(null)
    const statusChart = ref(null)
    const systemChart = ref(null)
    const timeChart = ref(null)
    let trendChartInstance = null
    let statusChartInstance = null
    let systemChartInstance = null
    let timeChartInstance = null

    // 获取系统列表
    const loadSystemOptions = async () => {
      try {
        const response = await systemApi.getSystemList({ pageSize: 1000 })
        systemOptions.value = response.data.records || []
      } catch (error) {
        console.error('获取系统列表失败:', error)
      }
    }

    // 加载采集统计数据
    const loadCollectStats = async () => {
      loading.value = true
      try {
        const params = {
          ...searchForm,
          page: pagination.page,
          size: pagination.size,
          startTime: searchForm.dateRange?.[0],
          endTime: searchForm.dateRange?.[1]
        }
        delete params.dateRange

        const response = await statisticsApi.getCollectStatistics(params)
        if (response.data) {
          detailStats.value = response.data.records || []
          pagination.total = response.data.total || 0
          
          // 更新概览数据
          Object.assign(overview, response.data.overview || {})
          
          // 更新TOP任务数据
          if (response.data.topTasks) {
            Object.assign(topTasks, response.data.topTasks)
          }
        }
      } catch (error) {
        console.error('获取采集统计数据失败:', error)
        ElMessage.error('获取数据失败')
      } finally {
        loading.value = false
      }
    }

    // 初始化图表
    const initCharts = () => {
      nextTick(() => {
        // 采集任务执行趋势图
        if (trendChart.value) {
          trendChartInstance = echarts.init(trendChart.value)
          const trendOption = {
            title: {
              text: '采集任务执行趋势',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis'
            },
            legend: {
              top: 30,
              data: ['成功任务', '失败任务', '总任务数']
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: ['01-08', '01-09', '01-10', '01-11', '01-12', '01-13', '01-14', '01-15']
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                name: '成功任务',
                type: 'line',
                data: [145, 152, 148, 167, 156, 162, 158, 165],
                itemStyle: { color: '#67C23A' },
                smooth: true
              },
              {
                name: '失败任务',
                type: 'line',
                data: [12, 8, 15, 11, 14, 9, 13, 10],
                itemStyle: { color: '#F56C6C' },
                smooth: true
              },
              {
                name: '总任务数',
                type: 'line',
                data: [157, 160, 163, 178, 170, 171, 171, 175],
                itemStyle: { color: '#409EFF' },
                smooth: true
              }
            ]
          }
          trendChartInstance.setOption(trendOption)
        }

        // 任务状态分布图
        if (statusChart.value) {
          statusChartInstance = echarts.init(statusChart.value)
          const statusOption = {
            title: {
              text: '任务状态分布',
              left: 'center'
            },
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} ({d}%)'
            },
            series: [
              {
                name: '任务状态',
                type: 'pie',
                radius: ['40%', '70%'],
                data: [
                  { value: overview.successTasks, name: '成功', itemStyle: { color: '#67C23A' } },
                  { value: overview.failedTasks, name: '失败', itemStyle: { color: '#F56C6C' } },
                  { value: 15, name: '执行中', itemStyle: { color: '#E6A23C' } },
                  { value: 8, name: '已取消', itemStyle: { color: '#909399' } }
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
          statusChartInstance.setOption(statusOption)
        }

        // 系统采集量分布图
        if (systemChart.value) {
          systemChartInstance = echarts.init(systemChart.value)
          const systemOption = {
            title: {
              text: '系统采集量分布',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: ['CRM系统', 'ERP系统', 'OA系统', '监控系统', '订单系统']
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                type: 'bar',
                data: [320, 280, 190, 150, 120],
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
          systemChartInstance.setOption(systemOption)
        }

        // 执行时间分析图
        if (timeChart.value) {
          timeChartInstance = echarts.init(timeChart.value)
          const timeOption = {
            title: {
              text: '执行时间分析',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis'
            },
            legend: {
              top: 30,
              data: ['平均时间', '最大时间', '最小时间']
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00']
            },
            yAxis: {
              type: 'value',
              axisLabel: {
                formatter: '{value}s'
              }
            },
            series: [
              {
                name: '平均时间',
                type: 'line',
                data: [35, 28, 45, 52, 48, 42],
                itemStyle: { color: '#409EFF' }
              },
              {
                name: '最大时间',
                type: 'line',
                data: [89, 76, 125, 145, 132, 108],
                itemStyle: { color: '#F56C6C' }
              },
              {
                name: '最小时间',
                type: 'line',
                data: [15, 12, 18, 22, 20, 16],
                itemStyle: { color: '#67C23A' }
              }
            ]
          }
          timeChartInstance.setOption(timeOption)
        }
      })
    }

    // 初始化模拟数据 - 已移除，数据现在通过API获取
    // const initMockData = () => {
    //   // 所有数据现在通过 loadCollectStats() API 获取
    // }

    // 事件处理
    const handleSearch = () => {
      pagination.page = 1
      loadCollectStats()
    }

    const handleReset = () => {
      Object.assign(searchForm, {
        dateRange: [],
        systemId: '',
        environment: '',
        status: ''
      })
      handleSearch()
    }

    const handleRefresh = () => {
      loadCollectStats()
    }

    const handleSizeChange = (size) => {
      pagination.size = size
      loadCollectStats()
    }

    const handleCurrentChange = (page) => {
      pagination.page = page
      loadCollectStats()
    }

    const handleGenerateReport = async () => {
      try {
        const params = {
          ...searchForm,
          startTime: searchForm.dateRange?.[0],
          endTime: searchForm.dateRange?.[1]
        }
        delete params.dateRange
        
        await statisticsApi.getCollectStatistics(params)
        ElMessage.success('采集统计报告生成成功')
        handleRefresh()
      } catch (error) {
        console.error('生成报告失败:', error)
        ElMessage.error('生成报告失败')
      }
    }

    const handleExportReport = () => {
      ElMessage.info('导出功能开发中...')
    }

    const handleViewDetail = (row) => {
      ElMessage.info('查看详情功能开发中...')
    }

    const handleAnalyze = (row) => {
      ElMessage.info('分析功能开发中...')
    }

    // 辅助函数
    const getEnvironmentType = (env) => {
      const types = { dev: 'info', test: 'warning', pre: 'danger', prod: 'success' }
      return types[env] || 'info'
    }

    const getEnvironmentText = (env) => {
      const texts = { dev: '开发环境', test: '测试环境', pre: '预生产', prod: '生产环境' }
      return texts[env] || env
    }

    const getProgressStatus = (rate) => {
      if (rate >= 95) return 'success'
      if (rate >= 80) return 'warning'
      return 'exception'
    }

    const getFailureRateType = (rate) => {
      if (rate >= 20) return 'danger'
      if (rate >= 10) return 'warning'
      return 'info'
    }

    // 表格汇总
    const getSummaries = (param) => {
      const { columns, data } = param
      const sums = []
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = '合计'
          return
        }
        
        const values = data.map(item => Number(item[column.property]))
        if (!values.every(value => isNaN(value))) {
          if (column.property === 'successRate') {
            const totalExecutions = data.reduce((sum, item) => sum + item.totalExecutions, 0)
            const totalSuccess = data.reduce((sum, item) => sum + item.successCount, 0)
            sums[index] = totalExecutions > 0 ? ((totalSuccess / totalExecutions) * 100).toFixed(1) + '%' : '0%'
          } else {
            sums[index] = values.reduce((prev, curr) => {
              const value = Number(curr)
              if (!isNaN(value)) {
                return prev + curr
              } else {
                return prev
              }
            }, 0)
          }
        } else {
          sums[index] = '-'
        }
      })
      return sums
    }

    // 初始化
    onMounted(() => {
      loadSystemOptions()
      loadCollectStats()
      initCharts()
    })

    return {
      loading,
      systemOptions,
      searchForm,
      overview,
      activeTab,
      topTasks,
      detailStats,
      pagination,
      trendChart,
      statusChart,
      systemChart,
      timeChart,
      handleSearch,
      handleReset,
      handleRefresh,
      handleSizeChange,
      handleCurrentChange,
      handleGenerateReport,
      handleExportReport,
      handleViewDetail,
      handleAnalyze,
      getEnvironmentType,
      getEnvironmentText,
      getProgressStatus,
      getFailureRateType,
      getSummaries
    }
  }
}
</script>

<style scoped>
.collect-stats {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
  color: #303133;
}

.page-actions {
  display: flex;
  gap: 10px;
}

.app-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  margin-bottom: 20px;
}

.card-title {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 500;
  color: #303133;
}

/* 统计卡片样式 */
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  color: white;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
  transform: translate(30px, -30px);
}

.stat-icon {
  font-size: 48px;
  margin-right: 20px;
  opacity: 0.9;
}

.stat-content {
  flex: 1;
}

.stat-number {
  font-size: 36px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 5px;
}

.stat-change,
.stat-rate {
  font-size: 12px;
  opacity: 0.8;
}

.stat-total {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-success {
  background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%);
}

.stat-failed {
  background: linear-gradient(135deg, #F56C6C 0%, #f78989 100%);
}

.stat-avg-time {
  background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%);
}

/* 图表容器 */
.chart-container {
  margin-top: 20px;
}

/* 分页容器 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 动画效果 */
.stat-card {
  transition: transform 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}
</style>