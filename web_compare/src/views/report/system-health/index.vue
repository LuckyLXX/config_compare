<template>
  <div class="system-health">
    <div class="page-header">
      <h2 class="page-title">系统健康报告</h2>
      <div class="page-actions">
        <el-button type="primary" @click="handleGenerateReport">
          <el-icon><Document /></el-icon>
          生成健康报告
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

    <!-- 系统状态概览 -->
    <div class="app-card">
      <h3 class="card-title">系统状态概览</h3>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="health-card health-overall">
            <div class="health-icon">
              <el-icon><Monitor /></el-icon>
            </div>
            <div class="health-content">
              <div class="health-score">{{ healthData.overallScore }}</div>
              <div class="health-label">整体健康分</div>
              <div class="health-status" :class="getHealthStatusClass(healthData.overallStatus)">
                {{ getHealthStatusText(healthData.overallStatus) }}
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="health-card health-cpu">
            <div class="health-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="health-content">
              <div class="health-score">{{ healthData.cpuUsage }}%</div>
              <div class="health-label">CPU使用率</div>
              <div class="health-trend">
                <el-icon><TrendCharts /></el-icon>
                <span :class="getTrendClass(healthData.cpuTrend)">{{ healthData.cpuTrend }}%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="health-card health-memory">
            <div class="health-icon">
              <el-icon><Odometer /></el-icon>
            </div>
            <div class="health-content">
              <div class="health-score">{{ healthData.memoryUsage }}%</div>
              <div class="health-label">内存使用率</div>
              <div class="health-trend">
                <el-icon><TrendCharts /></el-icon>
                <span :class="getTrendClass(healthData.memoryTrend)">{{ healthData.memoryTrend }}%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="health-card health-disk">
            <div class="health-icon">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <div class="health-content">
              <div class="health-score">{{ healthData.diskUsage }}%</div>
              <div class="health-label">磁盘使用率</div>
              <div class="health-trend">
                <el-icon><TrendCharts /></el-icon>
                <span :class="getTrendClass(healthData.diskTrend)">{{ healthData.diskTrend }}%</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 服务状态监控 -->
    <div class="app-card">
      <h3 class="card-title">服务状态监控</h3>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="service-status">
            <h4>核心服务</h4>
            <div class="service-list">
              <div 
                v-for="service in coreServices" 
                :key="service.name" 
                class="service-item"
              >
                <div class="service-name">{{ service.name }}</div>
                <div class="service-status-indicator">
                  <el-tag :type="getServiceStatusType(service.status)">
                    {{ getServiceStatusText(service.status) }}
                  </el-tag>
                </div>
                <div class="service-uptime">运行时间: {{ service.uptime }}</div>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="service-status">
            <h4>数据库服务</h4>
            <div class="service-list">
              <div 
                v-for="db in databaseServices" 
                :key="db.name" 
                class="service-item"
              >
                <div class="service-name">{{ db.name }}</div>
                <div class="service-status-indicator">
                  <el-tag :type="getServiceStatusType(db.status)">
                    {{ getServiceStatusText(db.status) }}
                  </el-tag>
                </div>
                <div class="service-metrics">
                  连接数: {{ db.connections }} | 响应时间: {{ db.responseTime }}ms
                </div>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="service-status">
            <h4>网络服务</h4>
            <div class="service-list">
              <div 
                v-for="network in networkServices" 
                :key="network.name" 
                class="service-item"
              >
                <div class="service-name">{{ network.name }}</div>
                <div class="service-status-indicator">
                  <el-tag :type="getServiceStatusType(network.status)">
                    {{ getServiceStatusText(network.status) }}
                  </el-tag>
                </div>
                <div class="service-metrics">
                  延迟: {{ network.latency }}ms | 丢包率: {{ network.packetLoss }}%
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 性能监控图表 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">系统资源使用趋势</h3>
          <div class="chart-container">
            <div ref="resourceChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">响应时间监控</h3>
          <div class="chart-container">
            <div ref="responseChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 告警信息 -->
    <div class="app-card">
      <h3 class="card-title">系统告警</h3>
      <div class="alert-summary">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="alert-stat critical">
              <div class="alert-count">{{ alertStats.critical }}</div>
              <div class="alert-label">紧急告警</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="alert-stat warning">
              <div class="alert-count">{{ alertStats.warning }}</div>
              <div class="alert-label">警告</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="alert-stat info">
              <div class="alert-count">{{ alertStats.info }}</div>
              <div class="alert-label">提示</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="alert-stat resolved">
              <div class="alert-count">{{ alertStats.resolved }}</div>
              <div class="alert-label">已解决</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <el-table
        :data="alertList"
        v-loading="alertLoading"
        stripe
        style="width: 100%; margin-top: 20px;"
      >
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlertLevelType(row.level)">{{ getAlertLevelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="告警标题" width="200" show-overflow-tooltip />
        <el-table-column prop="message" label="告警内容" show-overflow-tooltip />
        <el-table-column prop="source" label="告警来源" width="150" />
        <el-table-column prop="createTime" label="发生时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlertStatusType(row.status)">{{ getAlertStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewAlert(row)">
              <el-icon><View /></el-icon>
              查看
            </el-button>
            <el-button 
              link 
              type="success" 
              @click="handleResolveAlert(row)" 
              v-if="row.status !== 'resolved'"
            >
              <el-icon><Check /></el-icon>
              解决
            </el-button>
            <el-button link type="warning" @click="handleIgnoreAlert(row)">
              <el-icon><CloseBold /></el-icon>
              忽略
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="alertPagination.page"
          v-model:page-size="alertPagination.size"
          :total="alertPagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleAlertSizeChange"
          @current-change="handleAlertCurrentChange"
        />
      </div>
    </div>

    <!-- 系统信息 -->
    <div class="app-card">
      <h3 class="card-title">系统信息</h3>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="系统版本">{{ systemInfo.version }}</el-descriptions-item>
        <el-descriptions-item label="运行时间">{{ systemInfo.uptime }}</el-descriptions-item>
        <el-descriptions-item label="部署环境">{{ systemInfo.environment }}</el-descriptions-item>
        <el-descriptions-item label="JVM版本">{{ systemInfo.jvmVersion }}</el-descriptions-item>
        <el-descriptions-item label="内存使用">{{ systemInfo.memoryInfo }}</el-descriptions-item>
        <el-descriptions-item label="线程数">{{ systemInfo.threadCount }}</el-descriptions-item>
        <el-descriptions-item label="数据库连接">{{ systemInfo.dbConnections }}</el-descriptions-item>
        <el-descriptions-item label="最后更新">{{ systemInfo.lastUpdate }}</el-descriptions-item>
        <el-descriptions-item label="系统状态">
          <el-tag :type="getSystemStatusType(systemInfo.status)">{{ systemInfo.status }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Download,
  Refresh,
  Monitor,
  Cpu,
  Odometer,
  FolderOpened,
  TrendCharts,
  View,
  Check,
  CloseBold
} from '@element-plus/icons-vue'
import { systemHealthApi } from '@/api/report'
import * as echarts from 'echarts'

export default {
  name: 'SystemHealth',
  components: {
    Document,
    Download,
    Refresh,
    Monitor,
    Cpu,
    Odometer,
    FolderOpened,
    TrendCharts,
    View,
    Check,
    CloseBold
  },
  setup() {
    // 响应式数据
    const loading = ref(false)
    const alertLoading = ref(false)
    let refreshTimer = null

    // 系统健康数据
    const healthData = reactive({
      overallScore: 88,
      overallStatus: 'good',
      cpuUsage: 45,
      cpuTrend: '+2.3',
      memoryUsage: 68,
      memoryTrend: '-1.2',
      diskUsage: 34,
      diskTrend: '+0.8'
    })

    // 服务状态数据
    const coreServices = ref([
      { name: '配置比对服务', status: 'running', uptime: '15天 8小时' },
      { name: '任务调度服务', status: 'running', uptime: '15天 8小时' },
      { name: '数据采集服务', status: 'running', uptime: '12天 3小时' },
      { name: '报告生成服务', status: 'warning', uptime: '2天 15小时' }
    ])

    const databaseServices = ref([
      { name: 'MySQL 主库', status: 'running', connections: 45, responseTime: 12 },
      { name: 'MySQL 从库', status: 'running', connections: 23, responseTime: 15 },
      { name: 'Redis 缓存', status: 'running', connections: 156, responseTime: 2 }
    ])

    const networkServices = ref([
      { name: '内部网络', status: 'running', latency: 5, packetLoss: 0.1 },
      { name: '外部API', status: 'running', latency: 45, packetLoss: 0.3 },
      { name: '数据中心网络', status: 'warning', latency: 120, packetLoss: 2.1 }
    ])

    // 告警数据
    const alertStats = reactive({
      critical: 2,
      warning: 8,
      info: 15,
      resolved: 42
    })

    const alertList = ref([])
    const alertPagination = reactive({
      page: 1,
      size: 10,
      total: 0
    })

    // 系统信息
    const systemInfo = reactive({
      version: 'v1.0.0',
      uptime: '15天 8小时 32分钟',
      environment: '生产环境',
      jvmVersion: 'OpenJDK 11.0.16',
      memoryInfo: '2.1GB / 4GB',
      threadCount: 158,
      dbConnections: '68 / 100',
      lastUpdate: '2024-01-15 10:30:25',
      status: '运行正常'
    })

    // 图表参考
    const resourceChart = ref(null)
    const responseChart = ref(null)
    let resourceChartInstance = null
    let responseChartInstance = null

    // 加载系统健康数据
    const loadHealthData = async () => {
      try {
        const response = await systemHealthApi.getSystemHealthMetrics()
        if (response.data) {
          Object.assign(healthData, response.data)
        }
      } catch (error) {
        console.error('获取系统健康数据失败:', error)
      }
    }

    // 加载告警数据
    const loadAlertData = async () => {
      alertLoading.value = true
      try {
        const params = {
          page: alertPagination.page,
          size: alertPagination.size
        }
        const response = await systemHealthApi.getSystemHealthReports(params)
        if (response.data) {
          alertList.value = response.data.records || []
          alertPagination.total = response.data.total || 0
        }
      } catch (error) {
        console.error('获取告警数据失败:', error)
        ElMessage.error('获取告警数据失败')
      } finally {
        alertLoading.value = false
      }
    }

    // 初始化图表
    const initCharts = () => {
      nextTick(() => {
        // 系统资源使用趋势图
        if (resourceChart.value) {
          resourceChartInstance = echarts.init(resourceChart.value)
          const resourceOption = {
            title: {
              text: '系统资源使用趋势',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis'
            },
            legend: {
              top: 30,
              data: ['CPU使用率', '内存使用率', '磁盘使用率']
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: ['09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00']
            },
            yAxis: {
              type: 'value',
              max: 100,
              axisLabel: {
                formatter: '{value}%'
              }
            },
            series: [
              {
                name: 'CPU使用率',
                type: 'line',
                data: [35, 42, 38, 45, 48, 43, 45],
                itemStyle: { color: '#409EFF' },
                smooth: true
              },
              {
                name: '内存使用率',
                type: 'line',
                data: [62, 65, 67, 68, 70, 69, 68],
                itemStyle: { color: '#67C23A' },
                smooth: true
              },
              {
                name: '磁盘使用率',
                type: 'line',
                data: [32, 33, 33, 34, 34, 34, 34],
                itemStyle: { color: '#E6A23C' },
                smooth: true
              }
            ]
          }
          resourceChartInstance.setOption(resourceOption)
        }

        // 响应时间监控图
        if (responseChart.value) {
          responseChartInstance = echarts.init(responseChart.value)
          const responseOption = {
            title: {
              text: '响应时间监控',
              left: 'center'
            },
            tooltip: {
              trigger: 'axis',
              axisPointer: {
                type: 'shadow'
              }
            },
            legend: {
              top: 30,
              data: ['平均响应时间', '最大响应时间']
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              data: ['配置比对', '数据采集', '报告生成', '任务调度', '系统监控']
            },
            yAxis: {
              type: 'value',
              axisLabel: {
                formatter: '{value}ms'
              }
            },
            series: [
              {
                name: '平均响应时间',
                type: 'bar',
                data: [45, 52, 38, 41, 28],
                itemStyle: { color: '#409EFF' }
              },
              {
                name: '最大响应时间',
                type: 'bar',
                data: [156, 245, 128, 178, 98],
                itemStyle: { color: '#F56C6C' }
              }
            ]
          }
          responseChartInstance.setOption(responseOption)
        }
      })
    }

    // 事件处理
    const handleRefresh = () => {
      loadHealthData()
      loadAlertData()
    }

    const handleGenerateReport = async () => {
      try {
        await systemHealthApi.getSystemHealthReports({})
        ElMessage.success('系统健康报告生成成功')
        handleRefresh()
      } catch (error) {
        console.error('生成报告失败:', error)
        ElMessage.error('生成报告失败')
      }
    }

    const handleExportReport = () => {
      ElMessage.info('导出功能开发中...')
    }

    const handleAlertSizeChange = (size) => {
      alertPagination.size = size
      loadAlertData()
    }

    const handleAlertCurrentChange = (page) => {
      alertPagination.page = page
      loadAlertData()
    }

    const handleViewAlert = (row) => {
      ElMessage.info('查看告警详情功能开发中...')
    }

    const handleResolveAlert = async (row) => {
      try {
        await ElMessageBox.confirm('确定要标记为已解决吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        ElMessage.success('告警已标记为已解决')
        loadAlertData()
      } catch {
        // 用户取消
      }
    }

    const handleIgnoreAlert = async (row) => {
      try {
        await ElMessageBox.confirm('确定要忽略该告警吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        ElMessage.success('告警已忽略')
        loadAlertData()
      } catch {
        // 用户取消
      }
    }

    // 辅助函数
    const getHealthStatusClass = (status) => {
      const classes = {
        excellent: 'status-excellent',
        good: 'status-good',
        warning: 'status-warning',
        critical: 'status-critical'
      }
      return classes[status] || 'status-good'
    }

    const getHealthStatusText = (status) => {
      const texts = {
        excellent: '优秀',
        good: '良好',
        warning: '警告',
        critical: '严重'
      }
      return texts[status] || '良好'
    }

    const getTrendClass = (trend) => {
      const value = parseFloat(trend)
      if (value > 0) return 'trend-up'
      if (value < 0) return 'trend-down'
      return 'trend-stable'
    }

    const getServiceStatusType = (status) => {
      const types = {
        running: 'success',
        warning: 'warning',
        stopped: 'danger',
        maintenance: 'info'
      }
      return types[status] || 'info'
    }

    const getServiceStatusText = (status) => {
      const texts = {
        running: '运行中',
        warning: '警告',
        stopped: '已停止',
        maintenance: '维护中'
      }
      return texts[status] || status
    }

    const getAlertLevelType = (level) => {
      const types = {
        critical: 'danger',
        warning: 'warning',
        info: 'info'
      }
      return types[level] || 'info'
    }

    const getAlertLevelText = (level) => {
      const texts = {
        critical: '紧急',
        warning: '警告',
        info: '提示'
      }
      return texts[level] || level
    }

    const getAlertStatusType = (status) => {
      const types = {
        active: 'warning',
        resolved: 'success',
        ignored: 'info'
      }
      return types[status] || 'warning'
    }

    const getAlertStatusText = (status) => {
      const texts = {
        active: '活跃',
        resolved: '已解决',
        ignored: '已忽略'
      }
      return texts[status] || status
    }

    const getSystemStatusType = (status) => {
      if (status.includes('正常')) return 'success'
      if (status.includes('警告')) return 'warning'
      if (status.includes('错误')) return 'danger'
      return 'info'
    }

    // 定时刷新
    const startAutoRefresh = () => {
      refreshTimer = setInterval(() => {
        loadHealthData()
      }, 30000) // 30秒刷新一次
    }

    const stopAutoRefresh = () => {
      if (refreshTimer) {
        clearInterval(refreshTimer)
        refreshTimer = null
      }
    }

    // 初始化模拟数据
    const initMockData = () => {
      alertList.value = [
        {
          id: 1,
          level: 'critical',
          title: '数据库连接异常',
          message: 'MySQL主库连接数超过阈值，当前80个连接',
          source: '数据库监控',
          createTime: '2024-01-15 10:25:30',
          status: 'active'
        },
        {
          id: 2,
          level: 'warning',
          title: 'CPU使用率过高',
          message: '系统 CPU 使用率连续 5 分钟超过 80%',
          source: '系统监控',
          createTime: '2024-01-15 10:20:15',
          status: 'active'
        },
        {
          id: 3,
          level: 'info',
          title: '任务执行完成',
          message: '定时任务 "配置比对-生产环境" 执行完成',
          source: '任务调度',
          createTime: '2024-01-15 10:00:00',
          status: 'resolved'
        }
      ]
      alertPagination.total = 3
    }

    // 初始化
    onMounted(() => {
      initMockData()
      loadHealthData()
      loadAlertData()
      initCharts()
      startAutoRefresh()
    })

    onUnmounted(() => {
      stopAutoRefresh()
      if (resourceChartInstance) {
        resourceChartInstance.dispose()
      }
      if (responseChartInstance) {
        responseChartInstance.dispose()
      }
    })

    return {
      loading,
      alertLoading,
      healthData,
      coreServices,
      databaseServices,
      networkServices,
      alertStats,
      alertList,
      alertPagination,
      systemInfo,
      resourceChart,
      responseChart,
      handleRefresh,
      handleGenerateReport,
      handleExportReport,
      handleAlertSizeChange,
      handleAlertCurrentChange,
      handleViewAlert,
      handleResolveAlert,
      handleIgnoreAlert,
      getHealthStatusClass,
      getHealthStatusText,
      getTrendClass,
      getServiceStatusType,
      getServiceStatusText,
      getAlertLevelType,
      getAlertLevelText,
      getAlertStatusType,
      getAlertStatusText,
      getSystemStatusType
    }
  }
}
</script>

<style scoped>
.system-health {
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

/* 健康卡片样式 */
.health-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  color: white;
  position: relative;
  overflow: hidden;
}

.health-card::before {
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

.health-icon {
  font-size: 48px;
  margin-right: 20px;
  opacity: 0.9;
}

.health-content {
  flex: 1;
}

.health-score {
  font-size: 36px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.health-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 5px;
}

.health-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: inline-block;
}

.health-trend {
  display: flex;
  align-items: center;
  font-size: 12px;
  gap: 4px;
}

.health-overall {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.health-cpu {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.health-memory {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.health-disk {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.trend-up {
  color: #f56c6c;
}

.trend-down {
  color: #67c23a;
}

.trend-stable {
  color: #909399;
}

/* 服务状态样式 */
.service-status h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #303133;
}

.service-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  transition: all 0.3s;
}

.service-item:hover {
  border-color: #c0c4cc;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.service-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 5px;
}

.service-status-indicator {
  margin-bottom: 5px;
}

.service-uptime,
.service-metrics {
  font-size: 12px;
  color: #909399;
}

/* 告警统计样式 */
.alert-summary {
  margin-bottom: 20px;
}

.alert-stat {
  text-align: center;
  padding: 15px;
  border-radius: 6px;
  color: white;
}

.alert-count {
  font-size: 32px;
  font-weight: bold;
  margin-bottom: 5px;
}

.alert-label {
  font-size: 14px;
  opacity: 0.9;
}

.alert-stat.critical {
  background: linear-gradient(135deg, #f56c6c 0%, #ff7875 100%);
}

.alert-stat.warning {
  background: linear-gradient(135deg, #e6a23c 0%, #ffa940 100%);
}

.alert-stat.info {
  background: linear-gradient(135deg, #409eff 0%, #69c0ff 100%);
}

.alert-stat.resolved {
  background: linear-gradient(135deg, #67c23a 0%, #95de64 100%);
}

/* 状态样式 */
.status-excellent {
  color: #67c23a;
}

.status-good {
  color: #409eff;
}

.status-warning {
  color: #e6a23c;
}

.status-critical {
  color: #f56c6c;
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

/* 响应式设计 */
@media (max-width: 768px) {
  .health-card {
    flex-direction: column;
    text-align: center;
  }
  
  .health-icon {
    margin-right: 0;
    margin-bottom: 15px;
  }
  
  .service-status {
    margin-bottom: 20px;
  }
}

/* 动画效果 */
.health-card,
.service-item,
.alert-stat {
  transition: transform 0.3s ease;
}

.health-card:hover,
.alert-stat:hover {
  transform: translateY(-2px);
}

/* 加载动画 */
@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}

.loading {
  animation: pulse 1.5s ease-in-out infinite;
}
</style>