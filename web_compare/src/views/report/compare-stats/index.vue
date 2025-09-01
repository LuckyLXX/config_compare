<template>
  <div class="compare-stats">
    <div class="page-header">
      <h2 class="page-title">比对统计报告</h2>
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
        <el-form-item label="比对类型:">
          <el-select v-model="searchForm.compareType" placeholder="请选择类型" clearable style="width: 150px">
            <el-option label="JSON比对" value="json" />
            <el-option label="TEXT比对" value="text" />
            <el-option label="XML比对" value="xml" />
            <el-option label="数据库比对" value="database" />
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
      <h3 class="card-title">比对任务概览</h3>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-card stat-total">
            <div class="stat-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.totalCompares }}</div>
              <div class="stat-label">总比对数</div>
              <div class="stat-change">+{{ overview.compareChange }} 较上周</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-success">
            <div class="stat-icon">
              <el-icon><SuccessFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.consistentCompares }}</div>
              <div class="stat-label">一致比对</div>
              <div class="stat-rate">一致率: {{ overview.consistentRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-warning">
            <div class="stat-icon">
              <el-icon><WarningFilled /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.diffCompares }}</div>
              <div class="stat-label">差异比对</div>
              <div class="stat-rate">差异率: {{ overview.diffRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-card stat-avg-time">
            <div class="stat-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-number">{{ overview.avgCompareTime }}s</div>
              <div class="stat-label">平均比对时间</div>
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
          <h3 class="card-title">比对结果趋势</h3>
          <div class="chart-container">
            <div ref="trendChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">比对类型分布</h3>
          <div class="chart-container">
            <div ref="typeChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">差异类型统计</h3>
          <div class="chart-container">
            <div ref="diffChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="app-card">
          <h3 class="card-title">比对时间分析</h3>
          <div class="chart-container">
            <div ref="timeChart" style="width: 100%; height: 350px;"></div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 比对准确率分析 -->
    <div class="app-card">
      <h3 class="card-title">比对准确率分析</h3>
      <el-row :gutter="20">
        <el-col :span="8">
          <div class="accuracy-item">
            <div class="accuracy-header">
              <el-icon><Medal /></el-icon>
              <span>最高准确率</span>
            </div>
            <div class="accuracy-content">
              <div class="accuracy-value">{{ accuracyStats.highest.value }}%</div>
              <div class="accuracy-label">{{ accuracyStats.highest.system }}</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="accuracy-item">
            <div class="accuracy-header">
              <el-icon><TrendCharts /></el-icon>
              <span>平均准确率</span>
            </div>
            <div class="accuracy-content">
              <div class="accuracy-value">{{ accuracyStats.average.value }}%</div>
              <div class="accuracy-label">所有系统平均</div>
            </div>
          </div>
        </el-col>
        <el-col :span="8">
          <div class="accuracy-item">
            <div class="accuracy-header">
              <el-icon><WarningFilled /></el-icon>
              <span>最低准确率</span>
            </div>
            <div class="accuracy-content">
              <div class="accuracy-value">{{ accuracyStats.lowest.value }}%</div>
              <div class="accuracy-label">{{ accuracyStats.lowest.system }}</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- TOP 比对任务 -->
    <div class="app-card">
      <h3 class="card-title">TOP 比对任务</h3>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="比对次数最多" name="most-compared">
          <el-table :data="topTasks.mostCompared" stripe style="width: 100%">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="taskName" label="任务名称" width="200" />
            <el-table-column prop="systemName" label="系统" width="150" />
            <el-table-column prop="compareCount" label="比对次数" width="120" />
            <el-table-column prop="consistentRate" label="一致率" width="120">
              <template #default="{ row }">
                <el-progress :percentage="row.consistentRate" :status="getConsistentStatus(row.consistentRate)" :stroke-width="6" />
              </template>
            </el-table-column>
            <el-table-column prop="avgTime" label="平均时间" width="120" />
            <el-table-column prop="lastCompared" label="最后比对" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="差异最多" name="most-differences">
          <el-table :data="topTasks.mostDifferences" stripe style="width: 100%">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="taskName" label="任务名称" width="200" />
            <el-table-column prop="systemName" label="系统" width="150" />
            <el-table-column prop="diffCount" label="差异数量" width="120" />
            <el-table-column prop="diffRate" label="差异率" width="120">
              <template #default="{ row }">
                <el-tag :type="getDiffRateType(row.diffRate)">{{ row.diffRate }}%</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="avgDiffItems" label="平均差异项" width="120" />
            <el-table-column prop="lastDiff" label="最后差异" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="比对时间最长" name="longest-time">
          <el-table :data="topTasks.longestTime" stripe style="width: 100%">
            <el-table-column prop="rank" label="排名" width="80" />
            <el-table-column prop="taskName" label="任务名称" width="200" />
            <el-table-column prop="systemName" label="系统" width="150" />
            <el-table-column prop="maxTime" label="最长时间" width="120" />
            <el-table-column prop="avgTime" label="平均时间" width="120" />
            <el-table-column prop="compareCount" label="比对次数" width="120" />
            <el-table-column prop="lastCompared" label="最后比对" />
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
        <el-table-column prop="compareType" label="比对类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getCompareTypeTag(row.compareType)">{{ getCompareTypeText(row.compareType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCompares" label="比对次数" width="100" />
        <el-table-column prop="consistentCount" label="一致次数" width="100" />
        <el-table-column prop="diffCount" label="差异次数" width="100" />
        <el-table-column prop="consistentRate" label="一致率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.consistentRate" :status="getConsistentStatus(row.consistentRate)" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column prop="avgCompareTime" label="平均时间" width="120" />
        <el-table-column prop="maxCompareTime" label="最长时间" width="120" />
        <el-table-column prop="avgDiffItems" label="平均差异项" width="120" />
        <el-table-column prop="lastCompared" label="最后比对" width="180" />
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
  DataAnalysis,
  SuccessFilled,
  WarningFilled,
  Timer,
  Medal,
  TrendCharts,
  View
} from '@element-plus/icons-vue'
import { statisticsApi } from '@/api/report'
import { systemApi } from '@/api/system'
import * as echarts from 'echarts'

export default {
  name: 'CompareStats',
  components: {
    Document, Download, Refresh, Search, DataAnalysis,
    SuccessFilled, WarningFilled, Timer, Medal, TrendCharts, View
  },
  setup() {
    const loading = ref(false)
    const systemOptions = ref([])
    const searchForm = reactive({
      dateRange: [],
      systemId: '',
      environment: '',
      compareType: ''
    })
    
    const overview = reactive({
      totalCompares: 2456,
      compareChange: 45,
      consistentCompares: 2234,
      consistentRate: 91.0,
      diffCompares: 222,
      diffRate: 9.0,
      avgCompareTime: 8.5,
      timeChange: -0.8
    })
    
    const activeTab = ref('most-compared')
    const topTasks = reactive({ mostCompared: [], mostDifferences: [], longestTime: [] })
    const detailStats = ref([])
    const pagination = reactive({ page: 1, size: 20, total: 0 })
    const accuracyStats = reactive({
      highest: { value: 98.5, system: 'CRM系统' },
      average: { value: 91.2 },
      lowest: { value: 76.8, system: '监控系统' }
    })
    
    const trendChart = ref(null)
    const typeChart = ref(null)
    const diffChart = ref(null)
    const timeChart = ref(null)
    
    const loadCompareStats = async () => {
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
        const response = await statisticsApi.getCompareStatistics(params)
        if (response.data) {
          detailStats.value = response.data.records || []
          pagination.total = response.data.total || 0
          Object.assign(overview, response.data.overview || {})
        }
      } catch (error) {
        ElMessage.error('获取数据失败')
      } finally {
        loading.value = false
      }
    }
    
    const initCharts = () => {
      nextTick(() => {
        if (trendChart.value) {
          const instance = echarts.init(trendChart.value)
          instance.setOption({
            title: { text: '比对结果趋势', left: 'center' },
            tooltip: { trigger: 'axis' },
            legend: { top: 30, data: ['一致比对', '差异比对'] },
            xAxis: { type: 'category', data: ['01-08', '01-09', '01-10', '01-11', '01-12', '01-13', '01-14', '01-15'] },
            yAxis: { type: 'value' },
            series: [
              { name: '一致比对', type: 'line', data: [298, 312, 287, 325, 304, 318, 295, 342], itemStyle: { color: '#67C23A' } },
              { name: '差异比对', type: 'line', data: [32, 28, 41, 35, 38, 29, 45, 33], itemStyle: { color: '#F56C6C' } }
            ]
          })
        }
      })
    }
    
    return {
      loading, systemOptions, searchForm, overview, activeTab, topTasks, detailStats, pagination, accuracyStats,
      trendChart, typeChart, diffChart, timeChart,
      handleSearch: () => { pagination.page = 1; loadCompareStats() },
      handleReset: () => { Object.assign(searchForm, { dateRange: [], systemId: '', environment: '', compareType: '' }); loadCompareStats() },
      handleRefresh: loadCompareStats,
      handleSizeChange: (size) => { pagination.size = size; loadCompareStats() },
      handleCurrentChange: (page) => { pagination.page = page; loadCompareStats() },
      handleGenerateReport: () => ElMessage.success('生成报告功能开发中...'),
      handleExportReport: () => ElMessage.info('导出功能开发中...'),
      handleViewDetail: () => ElMessage.info('查看详情功能开发中...'),
      handleAnalyze: () => ElMessage.info('分析功能开发中...'),
      getEnvironmentType: (env) => ({ dev: 'info', test: 'warning', pre: 'danger', prod: 'success' }[env] || 'info'),
      getEnvironmentText: (env) => ({ dev: '开发环境', test: '测试环境', pre: '预生产', prod: '生产环境' }[env] || env),
      getCompareTypeTag: (type) => ({ json: 'primary', text: 'success', xml: 'warning', database: 'info' }[type] || 'info'),
      getCompareTypeText: (type) => ({ json: 'JSON', text: 'TEXT', xml: 'XML', database: '数据库' }[type] || type),
      getConsistentStatus: (rate) => rate >= 95 ? 'success' : rate >= 80 ? 'warning' : 'exception',
      getDiffRateType: (rate) => rate >= 20 ? 'danger' : rate >= 10 ? 'warning' : 'info',
      getSummaries: () => ['合计', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-', '-']
    }
  }
}
</script>

<style scoped>
.compare-stats { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-title { margin: 0; font-size: 24px; font-weight: 500; color: #303133; }
.page-actions { display: flex; gap: 10px; }
.app-card { background: #fff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); padding: 20px; margin-bottom: 20px; }
.card-title { margin: 0 0 20px 0; font-size: 18px; font-weight: 500; color: #303133; }
.stat-card { display: flex; align-items: center; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1); color: white; }
.stat-icon { font-size: 48px; margin-right: 20px; }
.stat-content { flex: 1; }
.stat-number { font-size: 36px; font-weight: bold; margin-bottom: 8px; }
.stat-label { font-size: 14px; opacity: 0.9; margin-bottom: 5px; }
.stat-change, .stat-rate { font-size: 12px; opacity: 0.8; }
.stat-total { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.stat-success { background: linear-gradient(135deg, #67C23A 0%, #85ce61 100%); }
.stat-warning { background: linear-gradient(135deg, #E6A23C 0%, #ebb563 100%); }
.stat-avg-time { background: linear-gradient(135deg, #409EFF 0%, #69c0ff 100%); }
.accuracy-item { text-align: center; padding: 20px; border: 1px solid #ebeef5; border-radius: 8px; }
.accuracy-header { display: flex; align-items: center; justify-content: center; margin-bottom: 15px; color: #409EFF; }
.accuracy-value { font-size: 28px; font-weight: bold; color: #303133; margin-bottom: 5px; }
.accuracy-label { font-size: 14px; color: #909399; }
.chart-container { margin-top: 20px; }
.pagination-container { display: flex; justify-content: center; margin-top: 20px; }
</style>